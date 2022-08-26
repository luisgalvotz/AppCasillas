package com.app.casillas

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import java.sql.Connection

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    var lat : Double? = null
    var long : Double? = null
    var clave : String? = null
    var notified : Boolean = false
    var timerOn : Boolean = false
    var countDownTime : Long = 30000

    val channelID = "AppCasillas"
    val channelName = "AppCasillas"

    private lateinit var connection: Connection

    private lateinit var mMap: GoogleMap
    private lateinit var mCircle: Circle
    private lateinit var ubicacionActual: Location
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var timer: CountDownTimer

    private lateinit var loadingDialog: LoadingDialog

    private lateinit var sp : SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        val bundle : Bundle? = intent.extras
        lat = bundle!!.getDouble("LAT")
        long = bundle.getDouble("LONG")
        clave = bundle.getString("CVE")

        sp = getSharedPreferences("Login", MODE_PRIVATE)

        loadingDialog = LoadingDialog(this)
        loadingDialog.startLoadingDialog()

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        if (savedInstanceState != null) {
            with(savedInstanceState) {
                val location = Location(LocationManager.NETWORK_PROVIDER)
                location.latitude = getDouble("Latitud")
                location.longitude = getDouble("Longitud")
                ubicacionActual = location

                countDownTime = getLong("CountDownTime")
                notified = getBoolean("Notified")
            }
            val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
            mapFragment.getMapAsync(this@MapsActivity)
        }
        else {
            setUbicacion()
        }

        timer = object : CountDownTimer(countDownTime, 1000) {
            override fun onTick(remainingTime: Long) {
                countDownTime = remainingTime
                Toast.makeText(this@MapsActivity, "Se registrará su asistencia en ${remainingTime/1000} segundos", Toast.LENGTH_SHORT)
            }

            override fun onFinish() {
                registrarVoto("https://conection387893.000webhostapp.com/registerVote.php?CVE=$clave")

                sp.edit().putBoolean("Logged", false).apply()
                sp.edit().putString("CVE", "").apply()

                val intent = Intent(this@MapsActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            }

        }

        locationRequest = LocationRequest.create().apply {
            interval = 4000
            fastestInterval = 2000
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (locationResult == null) {
                    return
                }
                for (location in locationResult.locations) {
                    if (this@MapsActivity::mCircle.isInitialized) {
                        val distancia = FloatArray(2)
                        Location.distanceBetween(location.latitude, location.longitude, mCircle.center.latitude, mCircle.center.longitude, distancia)
                        if (distancia[0] <= mCircle.radius) {
                            //Dentro del rango
                            if (!timerOn) {
                                timerOn = true
                                timer.start()
                                if (!notified) {
                                    notified = true
                                    enviarNotificacion()
                                }
                            }
                        }
                        else {
                            //Fuera del rango
                            if (timerOn) {
                                notified = false
                                countDownTime = 30000
                                timerOn = false
                                timer.cancel()
                            }
                        }
                    }

                }
            }
        }

        settingsAndLocationUpdates()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.run {
            putDouble("Latitud", ubicacionActual.latitude)
            putDouble("Longitud", ubicacionActual.longitude)
            putLong("CountDownTime", countDownTime)
            putBoolean("Notified", notified)
        }
    }

    private fun setUbicacion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        val ultimaUbicacion = fusedLocationProviderClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
        ultimaUbicacion.addOnSuccessListener { ubicacion ->
            if (ubicacion != null) {
                ubicacionActual = ubicacion

                val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
                mapFragment.getMapAsync(this@MapsActivity)
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        activarMiUbicacion()

        val latLngOrigen = LatLng(ubicacionActual.latitude, ubicacionActual.longitude)
        val latLngDestino = LatLng(lat!!, long!!)

        mMap.addMarker(MarkerOptions().position(latLngOrigen).title("Ubicación actual"))
        mMap.addMarker(MarkerOptions().position(latLngDestino).title("Casilla"))

        mCircle = mMap.addCircle(CircleOptions().center(latLngDestino).radius(100.0).fillColor(0x44ff0000).strokeWidth(0F))

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngOrigen, 15F))

        val URL = obtenerDireccionUrl(latLngOrigen, latLngDestino)
        obtenerDireccion(URL).execute()

        loadingDialog.dismissLoadingDialog()
    }

    private fun activarMiUbicacion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        mMap.isMyLocationEnabled = true
    }

    override fun onBackPressed() { }

    override fun onStop() {
        super.onStop()
        stopLocationUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (timerOn) {
            timerOn = false
            timer.cancel()
        }
    }

    private fun settingsAndLocationUpdates() {
        val request = LocationSettingsRequest.Builder().addLocationRequest(locationRequest).build()
        val client = LocationServices.getSettingsClient(this)

        val locationSettingsResponseTask = client.checkLocationSettings(request)
        locationSettingsResponseTask.addOnSuccessListener {
            startLocationUpdates()
        }
        locationSettingsResponseTask.addOnFailureListener { error ->
            if (error is ResolvableApiException) {
                val apiException: ResolvableApiException = error
                try {
                    apiException.startResolutionForResult(this, 1001)
                } catch (ex : IntentSender.SendIntentException) {
                    ex.printStackTrace()
                }
            }
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    private fun stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    private fun obtenerDireccionUrl(origen: LatLng, destino: LatLng): String {
        return "https://maps.googleapis.com/maps/api/directions/json?origin=${origen.latitude},${origen.longitude}&destination=${destino.latitude},${destino.longitude}&key=AIzaSyBnDn4Ce3fairDzTKrLENI4_39_nq5bceM"
    }

    @SuppressLint("StaticFieldLeak")
    inner class obtenerDireccion(val url: String): AsyncTask<Void, Void, List<List<LatLng>>>() {
        override fun doInBackground(vararg p0: Void?): List<List<LatLng>> {
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val data = response.body.string()
            val result = ArrayList<List<LatLng>>()
            try {
                val respObj = Gson().fromJson(data, GoogleMapDTO::class.java)

                val path = ArrayList<LatLng>()

                for (i in 0..(respObj.routes[0].legs[0].steps.size-1)) {
                    path.addAll(decodePolyline(respObj.routes[0].legs[0].steps[i].polyline.points))
                }
                result.add(path)
            }
            catch (e: Exception) {
                e.printStackTrace()
            }
            return result
        }

        override fun onPostExecute(result: List<List<LatLng>>?) {
            val lineoption = PolylineOptions()
            for (i in result!!.indices) {
                lineoption.addAll(result[i])
                lineoption.width(10f)
                lineoption.color(Color.BLUE)
                lineoption.geodesic(true)
            }
            mMap.addPolyline(lineoption)
        }
    }

    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val latLng = LatLng((lat.toDouble() / 1E5),(lng.toDouble() / 1E5))
            poly.add(latLng)
        }

        return poly
    }

    private fun enviarNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelID, channelName, importance)

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)

            val notification = NotificationCompat.Builder(this, channelID).also { notif ->
                notif.setContentTitle("Has llegado a tu casilla")
                notif.setContentText("No cerrar la aplicación")
                notif.setSmallIcon(R.mipmap.ic_launcher)
            }.build()

            val notificationManager = NotificationManagerCompat.from(applicationContext)
            notificationManager.notify(1, notification)
        }
    }

    private fun registrarVoto(url: String) {
        val stringRequest = StringRequest(com.android.volley.Request.Method.GET, url, { response ->

        }, { error ->
            Toast.makeText(this, error.toString(), Toast.LENGTH_LONG).show()
        })

        Volley.newRequestQueue(this).add(stringRequest)
    }

    /*private fun registrarVoto() {
        val c = ConnectionSQL()
        connection = c.connDb()!!

        if (c != null) {
            try {
                val sqlStatement = "UPDATE usuarios SET VOTO = 1 WHERE CVE = '$clave'"
                val smt = connection.createStatement()
                smt.executeQuery(sqlStatement)

                connection.close()

            } catch (e: java.lang.Exception) {
                Log.e("Error: ", e.message!!)

            } finally {
                sp.edit().putBoolean("Logged", false).apply()
                sp.edit().putString("CVE", "").apply()

                val intent = Intent(this@MapsActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }*/
}