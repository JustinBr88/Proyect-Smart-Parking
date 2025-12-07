package com.example.proyectosmartparking

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var parkingView: Pkview
    private val repository = Pkconsult("http://10.0.2.2:8000/") //URL a Cambiar el el de FastAPI que se va a usar
    private val handler = Handler(Looper.getMainLooper())
    private val refreshInterval: Long = 2000

    // Estos son datos que se usan por defecto si no conecta con el API
    private val estadoSimulado = mapOf(
        1 to mapOf(1 to 0, 2 to 1, 3 to 0, 4 to 0, 5 to 1, 6 to 1, 7 to 0, 8 to 0, 9 to 1),
        2 to mapOf(1 to 0, 2 to 1, 3 to 0)
    )

    private var modoSimulacion = false
    //Muestra la vista general de la actividad
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        parkingView = findViewById(R.id.pkview)

        startParkingPolling()
    }
    //Se encarga de mantener la conexión con el API y revisarlo periodicamente
    private fun startParkingPolling() {
        handler.post(object : Runnable {
            override fun run() {
                CoroutineScope(Dispatchers.Main).launch {
                    val estado = repository.getEstadoActual()
                    if (estado != null) {
                        parkingView.modoSimulacion = false
                        parkingView.estado = estado
                    } else {
                        modoSimulacion = true
                        parkingView.modoSimulacion = true
                        parkingView.estado = estadoSimulado
                        Toast.makeText(
                            this@MainActivity,
                            "Modo simulación activado: sin conexión a FastAPI",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                handler.postDelayed(this, refreshInterval)
            }
        })
    }
}