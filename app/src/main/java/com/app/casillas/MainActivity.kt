package com.app.casillas

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    var lat : Double? = null
    var long : Double? = null
    var clave : String? = null

    private lateinit var btnIngresar : Button
    private lateinit var txtClave : EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        txtClave = findViewById<EditText>(R.id.texto_clave)

        btnIngresar = findViewById<Button>(R.id.boton_ingresar)
        btnIngresar.setOnClickListener {
            buscarCasilla()
        }
    }

    private fun buscarCasilla() {
        clave = txtClave.text.toString()
        if (clave!!.isEmpty()) {
            txtClave.setError("Favor de llenar este campo")
        }
        else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    1
                )
            }
            else {
                abrirMapa("http://cursoswelearn.xyz/AppCasillas/readLocation.php?CVE=$clave")
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.isNotEmpty()) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Se requiere aceptar el permiso", Toast.LENGTH_SHORT).show()
            }
            else {
                Toast.makeText(this, "Permiso concedido", Toast.LENGTH_SHORT).show()
                abrirMapa("http://cursoswelearn.xyz/AppCasillas/readLocation.php?CVE=$clave")
            }
        }
    }

    private fun abrirMapa(url: String) {
        var claveEncontrada = false

        val stringRequest = StringRequest(Request.Method.GET, url, { response ->

            try {
                if (response.isNotEmpty()) {
                    val jsonObject = JSONObject(response)
                    lat = jsonObject.getDouble("lat")
                    long = jsonObject.getDouble("long")

                    claveEncontrada = true
                }
                else {
                    txtClave.setError("Clave no encontrada")
                }

            } catch (e: Exception) {
                Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show()

            } finally {
                if (claveEncontrada) {
                    val intent = Intent(this, MapsActivity::class.java)
                    intent.putExtra("LAT", lat)
                    intent.putExtra("LONG", long)
                    intent.putExtra("CVE", clave)
                    startActivityForResult(intent, 1)
                }

            }

        }, { error ->
            Toast.makeText(this, error.toString(), Toast.LENGTH_LONG).show()
        })

        Volley.newRequestQueue(this).add(stringRequest)
    }

}