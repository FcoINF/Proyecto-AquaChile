package com.duoc.aquaflow.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface MeteorologiaApi {
    @GET("v1/marine?current=wave_height,wave_period,wave_direction,sea_surface_temperature&timeformat=unixtime")
    suspend fun consultar(@Query("latitude") latitud: Double, @Query("longitude") longitud: Double): RespuestaMaritima

    companion object {
        fun crear(): MeteorologiaApi = Retrofit.Builder()
            .baseUrl("https://marine-api.open-meteo.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MeteorologiaApi::class.java)
    }
}

data class RespuestaMaritima(val latitude: Double?, val longitude: Double?, val current: PronosticoMaritimo?)

data class PronosticoMaritimo(
    val time: Long?,
    @SerializedName("wave_height") val altura: Double?,
    @SerializedName("wave_period") val periodo: Double?,
    @SerializedName("wave_direction") val direccion: Double?,
    @SerializedName("sea_surface_temperature") val temperatura: Double?
)
