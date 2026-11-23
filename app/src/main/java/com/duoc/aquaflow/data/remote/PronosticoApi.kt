package com.duoc.aquaflow.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface PronosticoApi {
    @GET("v1/forecast?current=temperature_2m,wind_speed_10m&daily=temperature_2m_min,temperature_2m_max,precipitation_probability_max&wind_speed_unit=kn&forecast_days=1&timezone=auto")
    suspend fun consultar(@Query("latitude") latitud: Double, @Query("longitude") longitud: Double): PronosticoUbicacion

    companion object {
        fun crear(): PronosticoApi = Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PronosticoApi::class.java)
    }
}

data class PronosticoUbicacion(val current: TiempoActual?, val daily: TiempoDiario?)

data class TiempoActual(
    @SerializedName("temperature_2m") val temperatura: Double?,
    @SerializedName("wind_speed_10m") val viento: Double?
)

data class TiempoDiario(
    @SerializedName("temperature_2m_min") val minima: List<Double?>?,
    @SerializedName("temperature_2m_max") val maxima: List<Double?>?,
    @SerializedName("precipitation_probability_max") val lluvia: List<Int?>?
)
