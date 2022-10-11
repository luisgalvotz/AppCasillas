package com.app.casillas

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.Circle
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import org.json.JSONObject
import java.lang.Exception
import java.util.*
import kotlin.collections.HashMap

class ScannerActivity : AppCompatActivity() {

    var cve : String? = null
    var nombre : String? = null
    var seccion : Int = 0
    var codigoEscaneado : String? = null

    var ubicacionActual : Location = Location(LocationManager.NETWORK_PROVIDER)
    var ubicacionString : String? = null

    private lateinit var fusedLocationProviderClient : FusedLocationProviderClient

    private lateinit var txtClave : EditText
    private lateinit var btnBuscar : Button
    private lateinit var btnCodigo : Button
    private lateinit var btnActivar : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)

        txtClave = findViewById<EditText>(R.id.texto_cve_encargado)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        btnBuscar = findViewById<Button>(R.id.boton_busca)
        btnBuscar.setOnClickListener {
            cve = txtClave.text.toString()
            if (cve!!.isEmpty()) {
                txtClave.setError("Favor de llenar este campo")
            }
            else {
                loginEncargado("https://appcasillas.com/loginManager.php?CVE=$cve")
            }
        }

        btnCodigo = findViewById<Button>(R.id.boton_codigo)
        btnCodigo.setOnClickListener {
            escanearCodigo()
        }

        btnActivar = findViewById<Button>(R.id.boton_activ)
        btnActivar.setOnClickListener {
            activarCasilla("https://appcasillas.com/activateSection.php")
        }

        if (savedInstanceState != null) {
            with(savedInstanceState) {
                seccion = getInt("Seccion")
                ubicacionActual.latitude = getDouble("Latitud")
                ubicacionActual.longitude = getDouble("Longitud")
                btnCodigo.visibility = getInt("BtnCodigoV")
                btnActivar.visibility = getInt("BtnActivarV")
                btnActivar.isEnabled = getBoolean("BtnActivarE")
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.run {
            putInt("Seccion", seccion)
            putDouble("Latitud", ubicacionActual.latitude)
            putDouble("Longitud", ubicacionActual.longitude)
            putInt("BtnCodigoV", btnCodigo.visibility)
            putInt("BtnActivarV", btnActivar.visibility)
            putBoolean("BtnActivarE", btnActivar.isEnabled)
        }
    }

    private fun loginEncargado(url: String) {
        var claveEncontrada = false

        val stringRequest = StringRequest(Request.Method.GET, url, { response ->

            try {
                if (response.isNotEmpty()) {
                    val jsonObject = JSONObject(response)
                    cve = jsonObject.getString("cve")
                    nombre = jsonObject.getString("nombre")
                    seccion = jsonObject.getInt("seccion")

                    claveEncontrada = true
                }
                else {
                    txtClave.setError("Clave no encontrada")
                }
            } catch (e: Exception) {
                Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show()
            } finally {
                if (claveEncontrada) {
                    btnCodigo.visibility = View.VISIBLE
                    btnActivar.visibility = View.VISIBLE

                    setUbicacion()
                    Toast.makeText(this, "Bienvenido $nombre", Toast.LENGTH_LONG).show()
                }
            }

        }, { error ->
            Toast.makeText(this, error.toString(), Toast.LENGTH_LONG).show()
        })

        Volley.newRequestQueue(this).add(stringRequest)
    }

    private fun setUbicacion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                1
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.isNotEmpty()) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Se requiere aceptar el permiso", Toast.LENGTH_SHORT).show()
            }
            else {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return
                }
                val ultimaUbicacion = fusedLocationProviderClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                ultimaUbicacion.addOnSuccessListener { ubicacion ->
                    if (ubicacion != null) {
                        ubicacionActual = ubicacion

                        val geocoder = Geocoder(this, Locale.getDefault())
                        Thread {
                            val direcciones = geocoder.getFromLocation(ubicacionActual.latitude, ubicacionActual.longitude, 1)
                            if (direcciones.size > 0) {
                                ubicacionString = direcciones[0].getAddressLine(0)
                            }
                        }.start()
                    }
                }
            }
        }
    }

    private fun escanearCodigo() {
        val integrador = IntentIntegrator(this)
        integrador.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES)
        integrador.setPrompt("Lector código QR")
        integrador.setCameraId(0)
        integrador.setBeepEnabled(true)
        integrador.setBarcodeImageEnabled(true)
        integrador.initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)

        if (result != null) {
            if (result.contents == null) {
                Toast.makeText(this, "Lectura cancelada", Toast.LENGTH_LONG).show()
            }
            else {
                Toast.makeText(this, result.contents, Toast.LENGTH_LONG).show()

                codigoEscaneado = result.contents
                validarActivacion("https://appcasillas.com/validActivation.php?SECCION=$seccion")
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun validarActivacion(url: String) {
        var activacionCorrecta = false

        val stringRequest = StringRequest(Request.Method.GET, url, { response ->

            try {
                if (response.isNotEmpty()) {
                    val jsonObject = JSONObject(response)
                    //val codigo = jsonObject.getString("codigo")
                    val lat = jsonObject.getDouble("lat")
                    val long = jsonObject.getDouble("long")

                    val distancia = FloatArray(2)
                    Location.distanceBetween(ubicacionActual.latitude, ubicacionActual.longitude, lat, long, distancia)
                    if (distancia[0] <= 100) { //aqui tambien se debe validar (codigo == codigoEscaneado)
                        activacionCorrecta = true
                    }
                    else {
                        Toast.makeText(this, "La activación no puede realizarse", Toast.LENGTH_LONG).show()
                    }

                }
            } catch (e: Exception) {
                Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show()
            } finally {
                if (activacionCorrecta) {
                    btnActivar.isEnabled = true

                    Toast.makeText(this, "Ya puede activar la casilla", Toast.LENGTH_LONG).show()
                }
            }

        }, { error ->
            Toast.makeText(this, error.toString(), Toast.LENGTH_LONG).show()
        })

        Volley.newRequestQueue(this).add(stringRequest)
    }

    private fun activarCasilla(url: String) {
        val stringRequest = object : StringRequest(Method.POST, url, { response ->

            try {
                Toast.makeText(this, "Gracias por activar la casilla $seccion", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show()
            } finally {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }

        }, { error ->
            Toast.makeText(this, error.toString(), Toast.LENGTH_LONG).show()
        }) {
            override fun getParams(): MutableMap<String, String>? {
                val params: MutableMap<String, String> = HashMap()
                params["SECCION"] = seccion.toString()
                params["UBICACION"] = ubicacionString!!
                return params
            }
        }

        Volley.newRequestQueue(this).add(stringRequest)
    }
}