package com.duoc.aquaflow.data.remote

import android.content.Context
import android.net.Uri
import com.duoc.aquaflow.data.local.EvidenciasFotos
import com.duoc.aquaflow.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.duoc.aquaflow.model.PreChequeo
import com.duoc.aquaflow.model.MedicionesInmersion
import com.duoc.aquaflow.model.OrigenMedicion
import com.duoc.aquaflow.model.RegistroCierre
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.model.Usuario
import java.io.IOException
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.DELETE
import retrofit2.http.POST
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.Query
import retrofit2.http.Streaming

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
    val comunicacionesOperativas: Boolean?,
    val meteorologia: CondicionesMaritimas? = null
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
        comunicacionesOperativas = apertura.comunicacionesOperativas,
        meteorologia = apertura.meteorologia
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

    @GET("api/faenas/{id}")
    suspend fun apertura(@Header("Authorization") auth: String, @Path("id") id: String): AperturaRemota

    @GET("api/faenas/{id}/expediente")
    suspend fun expediente(@Header("Authorization") auth: String, @Path("id") id: String): ExpedienteRemoto

    @PUT("api/faenas/{id}/expediente")
    suspend fun prechequeo(@Header("Authorization") auth: String, @Path("id") id: String,
        @Query("version") version: Long, @Query("publicar") publicar: Boolean,
        @Query("operacion") operacion: String?, @Body datos: PrechequeoRemoto): ExpedienteRemoto

    @DELETE("api/faenas/{id}/expediente")
    suspend fun retirar(@Header("Authorization") auth: String, @Path("id") id: String, @Query("version") version: Long)

    @Multipart
    @PUT("api/faenas/{id}/evidencias/{equipo}/{tipo}")
    suspend fun foto(@Header("Authorization") auth: String, @Path("id") id: String,
        @Path("equipo") equipo: EquipoRemoto, @Path("tipo") tipo: TipoFoto,
        @Query("version") version: Long, @Part archivo: MultipartBody.Part): FotoRemota

    @Streaming
    @GET("api/faenas/{id}/evidencias/{equipo}/{tipo}")
    suspend fun imagen(@Header("Authorization") auth: String, @Path("id") id: String,
        @Path("equipo") equipo: EquipoRemoto, @Path("tipo") tipo: TipoFoto): ResponseBody

    @DELETE("api/faenas/{id}/evidencias/{equipo}/{tipo}")
    suspend fun quitarFoto(@Header("Authorization") auth: String, @Path("id") id: String,
        @Path("equipo") equipo: EquipoRemoto, @Path("tipo") tipo: TipoFoto, @Query("version") version: Long)

    @POST("api/faenas/{id}/decision")
    suspend fun decidir(@Header("Authorization") auth: String, @Path("id") id: String,
        @Query("version") version: Long, @Body decision: SolicitudDecision): ExpedienteRemoto

    @PUT("api/faenas/{id}/inicio")
    suspend fun iniciar(@Header("Authorization") auth: String, @Path("id") id: String): ExpedienteRemoto

    @PUT("api/faenas/{id}/bitacora")
    suspend fun cerrar(@Header("Authorization") auth: String, @Path("id") id: String, @Body cierre: CierreRemoto): BitacoraRemota

    @PUT("api/faenas/{id}/bitacora/alertas/{tipo}/revision")
    suspend fun revisar(@Header("Authorization") auth: String, @Path("id") id: String, @Path("tipo") tipo: String): BitacoraRemota

    @GET("api/faenas/{id}/bloqueos")
    suspend fun bloqueos(@Header("Authorization") auth: String, @Path("id") id: String): List<BloqueoRemoto>
}

class FaenasRemotas(val url: String) {
    private val api = Retrofit.Builder()
        .baseUrl(url)
        .client(OkHttpClient.Builder().followRedirects(false).build())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(AquaFlowApi::class.java)

    @Volatile private var autorizacion: String? = null

    internal fun credencialesParaEnvio(cuenta: String) =
        CredencialesSincronizacion.Sesion(cuenta, url, sesionActiva())

    companion object {
        internal fun desdeSesion(sesion: CredencialesSincronizacion.Sesion) =
            FaenasRemotas(sesion.servidor).also { it.autorizacion = sesion.autorizacion }
    }

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
        val datos = AperturaDatos(apertura)
        val actual = expediente(apertura.id)
        if (actual != null) {
            check(actual.decision == null) { "Jefatura ya registró una decisión para esta faena." }
            check(actual.apertura.datos.copy(meteorologia = null) == datos.copy(meteorologia = null)) {
                "Retira el expediente enviado antes de cambiar la apertura. Se conservará tu borrador local."
            }
            return
        }
        consultar { api.guardar(sesionActiva(), apertura.id, datos) }
    }

    suspend fun listar(): List<AperturaRemota> =
        consultar { api.listar(sesionActiva()) }

    suspend fun cerrar(id: String, cierre: RegistroCierre) {
        val mediciones = requireNotNull(cierre.mediciones)
        consultar { api.cerrar(sesionActiva(), id, CierreRemoto(cierre.duracionMinutos, cierre.observaciones,
            cierre.revisionSalida, SaludRemota(requireNotNull(cierre.saludSalida)),
            CierreHistoricoRemoto(cierre.cerradoEn, mediciones))) }
    }

    suspend fun decidir(id: String, version: Long, decision: RegistroDecision) =
        consultar { api.decidir(sesionActiva(), id, version, SolicitudDecision(decision.decision, decision.motivo)); Unit }

    suspend fun iniciar(id: String) { consultar { api.iniciar(sesionActiva(), id) } }

    suspend fun retirar(id: String, version: Long) { consultar { api.retirar(sesionActiva(), id, version) } }

    suspend fun revisarAlerta(id: String, tipo: String) { consultar { api.revisar(sesionActiva(), id, tipo) } }

    suspend fun reservarEdicion(id: String, version: Long) {
        val actual = requireNotNull(expediente(id)) { "El expediente ya no está en el servidor." }
        check(actual.version == version && actual.decision == null) { "El expediente cambió. Actualiza el detalle." }
        if (actual.publicado) {
            consultar { api.prechequeo(sesionActiva(), id, version, false, null, actual.prechequeo) }
        }
    }

    private suspend fun expediente(id: String): ExpedienteRemoto? = try {
        consultar { api.expediente(sesionActiva(), id) }
    } catch (error: IllegalStateException) {
        if ((error.cause as? HttpException)?.code() != 404) throw error
        null
    }

    suspend fun enviarExpediente(
        resumen: ResumenFaena, operacion: String, leerFoto: suspend (String) -> ByteArray
    ) = withContext(Dispatchers.IO) {
        val id = resumen.apertura.id
        val previo = expediente(id)
        if (previo?.publicado == true && previo.operacion == operacion) return@withContext
        guardar(resumen.apertura)
        val checklist = requireNotNull(resumen.checklist)
        val asignadas = checklist.fotosAsociadasPorEquipo()
        val inspecciones = EquipoRemoto.entries.map { equipo ->
            val item = checklist.items.single { it.id == equipo.local }
            val captura = requireNotNull(asignadas[equipo.local]) { "Falta la foto de ${equipo.titulo}." }
            val metadatos = checklist.metadatosPorFoto[captura]
            InspeccionRemota(equipo, item.estado, item.notas,
                metadatos?.coincide.takeIf { metadatos?.referencia != null })
        }
        val datos = PrechequeoRemoto(inspecciones, SaludRemota(requireNotNull(resumen.encuesta)),
            AstRemoto(requireNotNull(resumen.ast)), requireNotNull(resumen.tareas).items)
        val auth = sesionActiva()
        check(previo?.decision == null) { "Jefatura ya registró una decisión para este expediente." }
        val actual = consultar { api.prechequeo(auth, id, previo?.version ?: 0, false, operacion,
            datos.copy(checklist = inspecciones.map { it.copy(coincideConReferencia = null) })) }
        var version = actual.version
        for (equipo in EquipoRemoto.entries) {
            val captura = asignadas.getValue(equipo.local)
            val referencia = checklist.metadatosPorFoto[captura]?.referencia
            for (tipo in TipoFoto.entries) {
                val ruta = if (tipo == TipoFoto.CAPTURA) captura else referencia
                val existe = actual.fotos.any { it.equipo == equipo && it.tipo == tipo }
                if (ruta == null) {
                    if (existe) {
                        consultar { api.quitarFoto(auth, id, equipo, tipo, version) }
                        version++
                    }
                    continue
                }
                val bytes = leerFoto(ruta)
                if (existe && consultar { api.imagen(auth, id, equipo, tipo).use { it.bytes() } }.contentEquals(bytes)) continue
                val parte = MultipartBody.Part.createFormData("archivo", "evidencia",
                    bytes.toRequestBody("application/octet-stream".toMediaType()))
                consultar { api.foto(auth, id, equipo, tipo, version, parte) }
                version++
            }
        }
        consultar { api.prechequeo(auth, id, version, true, operacion, datos) }
        Unit
    }

    suspend fun detalle(contexto: Context, id: String): DetalleRemoto = withContext(Dispatchers.IO) {
        val auth = sesionActiva()
        val actual = expediente(id)
        val apertura = actual?.apertura ?: consultar { api.apertura(auth, id) }
        val bloqueos = consultar { api.bloqueos(auth, id) }.map {
            IntentoBloqueado(it.responsable, fechaRemota(it.registradaEn), it.motivo, it.operacion)
        }
        if (actual == null) {
            val cierre = apertura.cierre?.let { RegistroCierre(0, "", true, apertura.supervisorACargo,
                it.cerradoEn, mediciones = it.mediciones()) }
            return@withContext DetalleRemoto(FaenaGuardada(ResumenFaena(apertura.local(), null, null, null),
                cierre = cierre, intentosBloqueados = bloqueos), null, null, emptyList())
        }
        val rutas = actual.fotos.associateWith { foto ->
            EvidenciasFotos.descargar(contexto, "$id-${foto.equipo}-${foto.tipo}-${foto.tomadaEn}") {
                consultar { api.imagen(auth, id, foto.equipo, foto.tipo).use { it.bytes() } }
            }
        }
        val capturas = rutas.filterKeys { it.tipo == TipoFoto.CAPTURA }.mapKeys { it.key.equipo.local }
        val metadatos = rutas.filterKeys { it.tipo == TipoFoto.CAPTURA }.entries.associate { (foto, ruta) ->
            ruta to MetadatosFoto(fechaRemota(foto.tomadaEn), foto.responsable,
                rutas.entries.firstOrNull { it.key.equipo == foto.equipo && it.key.tipo == TipoFoto.REFERENCIA }?.value,
                actual.prechequeo.checklist.firstOrNull { it.equipo == foto.equipo }?.coincideConReferencia)
        }
        val pre = actual.prechequeo
        val checklist = ChecklistFaena(id, pre.checklist.map {
            ItemEquipamiento(it.equipo.local, it.equipo.titulo, "", it.estado, it.notas.orEmpty())
        }, capturas.values.toList(), fotosPorEquipo = capturas, metadatosPorFoto = metadatos)
        val resumen = ResumenFaena(apertura.local(), checklist, pre.ast?.local(), pre.saludInicial?.local(),
            ChecklistTareas(pre.tareas))
        val decision = actual.decision?.let { RegistroDecision(it.resultado, it.motivo.orEmpty(), it.responsable) }
        val inicio = actual.inicio?.let { RegistroInicio(it.responsable, fechaRemota(it.registradaEn)) }
        DetalleRemoto(FaenaGuardada(resumen, decision, actual.bitacora?.local(), inicio, bloqueos),
            actual.version, actual.semaforo, actual.bitacora?.alertas.orEmpty(), actual.publicado)
    }

    private fun fechaRemota(valor: String): Long {
        val formato = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", java.util.Locale.ROOT)
        val fecha = Regex("""^(.*:\d{2})(?:\.(\d+))?(Z|[+-]\d{2}:\d{2})$""").matchEntire(valor)
            ?: error("Fecha de servidor no reconocida.")
        val (segundos, fraccion, zona) = fecha.destructured
        val offset = if (zona == "Z") "+0000" else zona.replace(":", "")
        return requireNotNull(formato.parse("$segundos.${fraccion.padEnd(3, '0').take(3)}$offset")).time
    }

    private fun sesionActiva(): String =
        checkNotNull(autorizacion) { "Vuelve a ingresar para conectarte al servidor." }

    private suspend fun <T> consultar(peticion: suspend () -> T): T {
        try {
            return peticion()
        } catch (e: HttpException) {
            val mensaje = when (e.code()) {
                401 -> "Usuario o contraseña incorrectos. Si ya habías ingresado, vuelve a iniciar sesión."
                403 -> "Tu perfil no tiene permiso para esta operación."
                404 -> "El servidor no encontró el registro solicitado."
                400, 409, 422 -> runCatching {
                    e.response()?.errorBody()?.use { JSONObject(it.string()).optString("detail") }
                }.getOrNull()?.takeIf { it.isNotBlank() } ?: "El servidor bloqueó esta operación."
                429 -> "Demasiados intentos. Espera un minuto y vuelve a intentar."
                else -> "No se pudo completar la operación en el servidor."
            }
            throw IllegalStateException(mensaje, e)
        } catch (e: IOException) {
            throw IllegalStateException("No se pudo conectar al servidor. Comprueba que esté activo y vuelve a intentar.", e)
        }
    }
}
