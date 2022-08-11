package com.app.casillas

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.sql.Connection

class MainActivity : AppCompatActivity() {

    var lat : Double? = null
    var long : Double? = null
    var clave : String? = null

    private lateinit var sp : SharedPreferences

    private lateinit var connection: Connection

    private lateinit var btnIngresar : Button
    private lateinit var txtClave : EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        txtClave = findViewById<EditText>(R.id.texto_clave)

        sp = getSharedPreferences("Login", MODE_PRIVATE)

        if (sp.getBoolean("Logged", false)) {
            clave = sp.getString("CVE", "")
            solicitarPermisos()
        }

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
            solicitarPermisos()
        }
    }

    private fun solicitarPermisos() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                1
            )
        }
        else {
            //abrirMapa("http://cursoswelearn.xyz/AppCasillas/readLocation.php?CVE=$clave")
            abrirMapaa()
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
                //abrirMapa("http://cursoswelearn.xyz/AppCasillas/readLocation.php?CVE=$clave")
                abrirMapaa()
            }
        }
    }

    private fun abrirMapaa() {
        val c = ConnectionSQL()
        connection = c.connDb()

        if (c != null) {
            try {
                val sqlStatement = "SELECT * FROM usuarios WHERE CVE=$clave"
                val smt = connection.createStatement()
                val set = smt.executeQuery(sqlStatement)

                while (set.next()) {
                    var id = set.getString(1)
                    var nom = set.getString(3)
                }
                connection.close()

            } catch (e: Exception) {
                Log.e("Error: ", e.message!!)
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
                    sp.edit().putBoolean("Logged", true).apply()
                    sp.edit().putString("CVE", clave).apply()

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