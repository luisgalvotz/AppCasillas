package com.app.casillas

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.lang.Exception

class ScannerActivity : AppCompatActivity() {

    var cve : String? = null
    var nombre : String? = null
    var seccion : Int? = null

    private lateinit var txtClave : EditText
    private lateinit var btnBuscar : Button
    private lateinit var btnCodigo : Button
    private lateinit var btnActivar : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)

        txtClave = findViewById<EditText>(R.id.texto_cve_encargado)

        btnBuscar = findViewById<Button>(R.id.boton_busca)
        btnBuscar.setOnClickListener {
            cve = txtClave.text.toString()
            if (cve!!.isEmpty()) {
                txtClave.setError("Favor de llenar este campo")
            }
            else {
                loginEncargado("https://conection387893.000webhostapp.com/loginManager.php?CVE=$cve")
            }
        }

        btnCodigo = findViewById<Button>(R.id.boton_codigo)
        btnCodigo.setOnClickListener {
            //funcion para escanear codigo
        }

        btnActivar = findViewById<Button>(R.id.boton_activ)
        btnActivar.setOnClickListener {
            activarCasilla("https://conection387893.000webhostapp.com/activateSection.php?SECCION=$seccion")
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
                    btnCodigo.isEnabled = true
                    btnActivar.isEnabled = true

                    //DIALOG DE BIENVENIDA AL ENCARGADO
                }
            }

        }, { error ->
            Toast.makeText(this, error.toString(), Toast.LENGTH_LONG).show()
        })

        Volley.newRequestQueue(this).add(stringRequest)
    }

    private fun activarCasilla(url: String) {
        val stringRequest = StringRequest(Request.Method.GET, url, { response ->

            try {
                //SE ACTIVARÁ LA CASILLA CORRECTA
            } catch (e: Exception) {
                Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show()
            } finally {
                //DIALOG DE CASILLA ACTIVADA

                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }

        }, { error ->
            Toast.makeText(this, error.toString(), Toast.LENGTH_LONG).show()
        })

        Volley.newRequestQueue(this).add(stringRequest)
    }
}