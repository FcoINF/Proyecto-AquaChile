package com.duoc.aquaflow.data.remote

import com.duoc.aquaflow.model.PreChequeo
import com.duoc.aquaflow.model.MedicionesInmersion
import com.duoc.aquaflow.model.OrigenMedicion
import com.duoc.aquaflow.model.RegistroCierre
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.model.Usuario
import java.io.IOException
import okhttp3.Credentials
import okhttp3.OkHttpClient
import org.json.JSONObject
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Path

data class AperturaRemota(
    val id: String,
    val supervisorACargo: String,
    val datos: AperturaDatos,
    val cierre: CierreHistoricoRemoto? = null
)

data class CierreHistoricoRemoto(
    val cerradoEn: Long,
    val tiempoFondoMinutos: Int,
    val profundidadMaximaMetros: Double,
    val descensoMaximo: Double,
    val ascensoMaximo: Double,
    val origen: OrigenMedicion
) {
    constructor(cerradoEn: Long, mediciones: MedicionesInmersion) : this(
        cerradoEn, mediciones.tiempoFondoMinutos, mediciones.profundidadMaximaMetros,
        mediciones.descensoMaximo, mediciones.ascensoMaximo, mediciones.origen
    )

    fun mediciones() = MedicionesInmersion(tiempoFondoMinutos, profundidadMaximaMetros, descensoMaximo, ascensoMaximo, origen)
}

data class AperturaDatos(
    val centroCultivo: String,
    val centroId: String,
    val buzoAsignado: String,
    val buzoId: String,
    val fecha: String,
    val hora: String,
    val edadBuzo: Int,
    val imcBuzo: Double,
    val puntajeCalbuco: Int?,
    val examenPreocupacionalVigente: Boolean?,
    val vigenciaExamenPreocupacional: String,
    val testEsfuerzoVigente: Boolean?,
    val vigenciaTestEsfuerzo: String,
    val buzoEmergenciaDisponible: Boolean?,
    val condicionesListas: Boolean,
    val embarcacionLista: Boolean?,
    val comunicacionesOperativas: Boolean?
) {
    constructor(apertura: PreChequeo) : this(
        centroCultivo = apertura.centroCultivo,
        centroId = apertura.centroId,
        buzoAsignado = apertura.buzoAsignado,
        buzoId = apertura.buzoId,
        fecha = apertura.fecha,
        hora = apertura.hora,
        edadBuzo = apertura.edadBuzo,
        imcBuzo = apertura.imcBuzo,
        puntajeCalbuco = apertura.puntajeCalbuco,
        examenPreocupacionalVigente = apertura.examenPreocupacionalVigente,
        vigenciaExamenPreocupacional = apertura.vigenciaExamenPreocupacional,
        testEsfuerzoVigente = apertura.testEsfuerzoVigente,
        vigenciaTestEsfuerzo = apertura.vigenciaTestEsfuerzo,
        buzoEmergenciaDisponible = apertura.buzoEmergenciaDisponible,
        condicionesListas = apertura.condicionesListas,
        embarcacionLista = apertura.embarcacionLista,
        comunicacionesOperativas = apertura.comunicacionesOperativas
    )
}

internal data class IdentidadRemota(val nombre: String, val rol: Rol)

internal interface AquaFlowApi {
    @GET("api/sesion")
    suspend fun sesion(@Header("Authorization") autorizacion: String): IdentidadRemota

    @PUT("api/faenas/{id}")
    suspend fun guardar(
        @Header("Authorization") autorizacion: String,
        @Path("id") id: String,
        @Body datos: AperturaDatos
    ): AperturaRemota

    @GET("api/faenas")
    suspend fun listar(@Header("Authorization") autorizacion: String): List<AperturaRemota>

    @PUT("api/faenas/{id}/cierre-historico")
    suspend fun publicarCierre(
        @Header("Authorization") autorizacion: String,
        @Path("id") id: String,
        @Body cierre: CierreHistoricoRemoto
    ): Unit
}

class FaenasRemotas(url: String) {
    private val api = Retrofit.Builder()
        .baseUrl(url)
        .client(OkHttpClient.Builder().followRedirects(false).build())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(AquaFlowApi::class.java)

    private var autorizacion: String? = null

    suspend fun ingresar(nombre: String, clave: String, perfil: Rol): Usuario {
        val encabezado = Credentials.basic(nombre, clave)
        val identidad = consultar { api.sesion(encabezado) }
        require(identidad.rol == perfil) { "Selecciona el perfil asociado a tu cuenta." }
        autorizacion = encabezado
        return Usuario(identidad.nombre, "", identidad.rol)
    }

    fun salir() {
        autorizacion = null
    }

    suspend fun guardar(apertura: PreChequeo) {
        consultar { api.guardar(sesionActiva(), apertura.id, AperturaDatos(apertura)) }
    }

    suspend fun listar(): List<AperturaRemota> =
        consultar { api.listar(sesionActiva()) }

    suspend fun publicarCierre(id: String, cierre: RegistroCierre) {
        val mediciones = requireNotNull(cierre.mediciones)
        consultar { api.publicarCierre(sesionActiva(), id, CierreHistoricoRemoto(cierre.cerradoEn, mediciones)) }
    }

    private fun sesionActiva(): String =
        checkNotNull(autorizacion) { "Vuelve a ingresar para conectarte al servidor." }

    private suspend fun <T> consultar(peticion: suspend () -> T): T {
        try {
            return peticion()
        } catch (e: HttpException) {
            val mensaje = when (e.code()) {
                401 -> "Usuario o PIN incorrectos. Si ya habías ingresado, vuelve a iniciar sesión."
                403 -> "Tu perfil no tiene permiso para esta operación."
                404 -> "El servidor no encontró el registro solicitado."
                400, 409, 422 -> runCatching {
                    e.response()?.errorBody()?.use { JSONObject(it.string()).optString("detail") }
                }.getOrNull()?.takeIf { it.isNotBlank() } ?: "El servidor bloqueó esta operación."
                else -> "No se pudo completar la operación en el servidor."
            }
            throw IllegalStateException(mensaje, e)
        } catch (e: IOException) {
            throw IllegalStateException("No se pudo conectar al servidor. Comprueba que esté activo y vuelve a intentar.", e)
        }
    }
}
