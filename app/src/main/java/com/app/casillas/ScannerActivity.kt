package com.app.casillas

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import java.lang.Exception

class ScannerActivity : AppCompatActivity() {

    var seccion : String? = null

    private lateinit var btnScan : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)

        btnScan = findViewById<Button>(R.id.boton_scan)
        btnScan.setOnClickListener {
            scanCodigo("https://conection387893.000webhostapp.com/activateSection.php?SECCION=$seccion")
        }
    }

    private fun scanCodigo(url: String) {
        val stringRequest = StringRequest(Request.Method.GET, url, { response ->

            try {
                //VERIFICAR SI SE ESCANEÓ CORRECTAMENTE EL CODIGO
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