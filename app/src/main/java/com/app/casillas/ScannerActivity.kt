package com.app.casillas

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import org.json.JSONObject
import java.lang.Exception

class ScannerActivity : AppCompatActivity() {

    var cve : String? = null
    var nombre : String? = null
    var seccion : Int? = null
    var codigo : String? = null

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
                loginEncargado("https://appcasillas.com/loginManager.php?CVE=$cve")
            }
        }

        btnCodigo = findViewById<Button>(R.id.boton_codigo)
        btnCodigo.setOnClickListener {
            escanearCodigo()
        }

        btnActivar = findViewById<Button>(R.id.boton_activ)
        btnActivar.setOnClickListener {
            activarCasilla("https://appcasillas.com/activateSection.php?SECCION=$seccion")
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
                    //codigo = jsonObject.getString("codigo")

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

                    Toast.makeText(this, "Bienvenido $nombre", Toast.LENGTH_LONG).show()
                }
            }

        }, { error ->
            Toast.makeText(this, error.toString(), Toast.LENGTH_LONG).show()
        })

        Volley.newRequestQueue(this).add(stringRequest)
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
                //se debe comparar el codigo escaneado (result.contents) con el codigo de la casilla (var codigo)
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun activarCasilla(url: String) {
        val stringRequest = StringRequest(Request.Method.GET, url, { response ->

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
        })

        Volley.newRequestQueue(this).add(stringRequest)
    }
}