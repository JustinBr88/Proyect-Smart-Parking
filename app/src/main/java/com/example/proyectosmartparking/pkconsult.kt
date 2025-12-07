package com.example.proyectosmartparking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

// Data class para el estado
typealias EstadoParking = Map<Int, Map<Int, Int>>

interface ParkingApi {
    @GET("status")
    suspend fun getStatus(): EstadoParking
}

class Pkconsult(baseUrl: String) {
    private val api = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ParkingApi::class.java)

    // Método para obtener el estado actual de los estacionamiento
    suspend fun getEstadoActual(): EstadoParking? {
        return withContext(Dispatchers.IO) {
            try {
                api.getStatus()
            } catch (e: Exception) {
                null
            }
        }
    }
}