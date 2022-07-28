package com.app.casillas

import android.Manifest
import android.annotation.SuppressLint
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.AsyncTask
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    var lat : Double? = null
    var long : Double? = null
    var clave : String? = null

    private lateinit var mMap: GoogleMap
    private lateinit var ubicacionActual: Location
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private lateinit var loadingDialog: LoadingDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        val bundle : Bundle? = intent.extras
        lat = bundle!!.getDouble("LAT")
        long = bundle.getDouble("LONG")
        clave = bundle.getString("CVE")

        loadingDialog = LoadingDialog(this)
        loadingDialog.startLoadingDialog()

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

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
                    //Log.d("MapsActivity", "Location: ${location.toString()}")
                    Toast.makeText(this@MapsActivity, "Location: ${location.latitude}, ${location.longitude}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        setUbicacion()
        settingsAndLocationUpdates()
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
        //val latLngDestino = LatLng(25.7422697, -100.3120796)

        mMap.addMarker(MarkerOptions().position(latLngOrigen).title("Ubicación actual"))
        mMap.addMarker(MarkerOptions().position(latLngDestino).title("Casilla"))

        val mCircle = mMap.addCircle(CircleOptions().center(latLngDestino).radius(100.0).fillColor(0x44ff0000).strokeWidth(0F))
        val distancia = FloatArray(2)
        Location.distanceBetween(latLngOrigen.latitude, latLngOrigen.longitude, mCircle.center.latitude, mCircle.center.longitude, distancia)
        if (distancia[0] > mCircle.radius) {
            Toast.makeText(this, "Fuera, distancia del centro: ${distancia[0]}", Toast.LENGTH_SHORT).show()
        }
        else {
            Toast.makeText(this, "Dentro, distancia del centro: ${distancia[0]}", Toast.LENGTH_SHORT).show()
        }

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

    override fun onStop() {
        super.onStop()
        stopLocationUpdates()
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
        return "https://maps.googleapis.com/maps/api/directions/json?origin=${origen.latitude},${origen.longitude}&destination=${destino.latitude},${destino.longitude}&key=AIzaSyDWXNDTULZTRa4K5YCf7d0N-bCjPJL8H5I"
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
}