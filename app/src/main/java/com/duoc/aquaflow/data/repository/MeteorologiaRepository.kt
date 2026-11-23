package com.duoc.aquaflow.data.repository

import com.duoc.aquaflow.data.remote.MeteorologiaApi
import com.duoc.aquaflow.model.CentroMaritimo
import com.duoc.aquaflow.model.CondicionesMaritimas
import java.io.IOException
import retrofit2.HttpException

sealed interface ResultadoMeteorologia {
    data class Disponible(val condiciones: CondicionesMaritimas) : ResultadoMeteorologia
    data object SinConexion : ResultadoMeteorologia
    data class Error(val mensaje: String) : ResultadoMeteorologia
}

class MeteorologiaRepository(
    private val api: MeteorologiaApi,
    private val hayRed: () -> Boolean,
    private val reloj: () -> Long = System::currentTimeMillis
) {
    suspend fun consultar(centro: CentroMaritimo): ResultadoMeteorologia {
        if (!hayRed()) return ResultadoMeteorologia.SinConexion
        return try {
            val respuesta = api.consultar(centro.latitud, centro.longitud)
            val pronostico = respuesta.current
            val latitud = respuesta.latitude
            val longitud = respuesta.longitude
            val hora = pronostico?.time
            if (pronostico == null || hora == null || hora <= 0 || latitud == null || longitud == null ||
                listOf(pronostico.altura, pronostico.periodo, pronostico.direccion, pronostico.temperatura)
                    .all { it == null }) {
                ResultadoMeteorologia.Error("No hay datos marítimos disponibles para este punto.")
            } else {
                ResultadoMeteorologia.Disponible(CondicionesMaritimas(latitud, longitud, reloj(), hora * 1000,
                    pronostico.altura, pronostico.periodo, pronostico.direccion, pronostico.temperatura))
            }
        } catch (error: IOException) {
            if (hayRed()) ResultadoMeteorologia.Error("No se pudo contactar con Open-Meteo.")
            else ResultadoMeteorologia.SinConexion
        } catch (error: HttpException) {
            ResultadoMeteorologia.Error("Open-Meteo no pudo responder (HTTP ${error.code()}).")
        } catch (error: com.google.gson.JsonParseException) {
            ResultadoMeteorologia.Error("Open-Meteo devolvió datos con un formato inesperado.")
        }
    }
}
