package com.duoc.aquaflow.data.remote

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.duoc.aquaflow.data.local.AquaFlowDatabase
import com.duoc.aquaflow.data.local.EnvioFaena
import com.duoc.aquaflow.data.local.EstadoEnvio
import com.duoc.aquaflow.data.local.EvidenciasFotos
import com.duoc.aquaflow.data.local.FragmentoFoto
import com.duoc.aquaflow.model.ResumenFaena
import com.duoc.aquaflow.model.fotosAsociadasPorEquipo
import com.google.gson.Gson
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class SincronizacionFaenas(private val contexto: Context, private val servidor: String) {
    private val room = AquaFlowDatabase.obtener(contexto)
    private val dao = room.sincronizacionDao()

    fun observar(cuenta: String) = dao.observar(cuenta, servidor)

    suspend fun iniciarSesion(cuenta: String, remoto: FaenasRemotas) = withContext(Dispatchers.IO) {
        CredencialesSincronizacion.guardar(contexto, remoto.credencialesParaEnvio(cuenta))
        val recuperacion = PeriodicWorkRequestBuilder<RecuperarEnviosWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(contexto).enqueueUniquePeriodicWork("recuperar-envios-faena",
            ExistingPeriodicWorkPolicy.KEEP, recuperacion)
        recuperarPendientes(cuenta)
    }

    fun cerrarSesion() {
        CredencialesSincronizacion.borrar(contexto)
        WorkManager.getInstance(contexto).cancelAllWorkByTag(ENVIO_FAENA)
    }

    suspend fun encolar(cuenta: String, resumen: ResumenFaena) = withContext(Dispatchers.IO) {
        val id = resumen.apertura.id
        require(id.isNotBlank() && resumen.apertura.supervisorACargo == cuenta)
        val checklist = requireNotNull(resumen.checklist)
        requireNotNull(resumen.encuesta)
        requireNotNull(resumen.ast)
        requireNotNull(resumen.tareas)
        val asignaciones = checklist.fotosAsociadasPorEquipo()
        val rutas = EquipoRemoto.entries.flatMap { equipo ->
            val captura = requireNotNull(asignaciones[equipo.local]) { "Falta la foto de ${equipo.titulo}." }
            listOfNotNull(captura, checklist.metadatosPorFoto[captura]?.referencia)
        }.distinct()
        val formulario = Gson().toJson(resumen)
        val envio = room.withTransaction {
            val anterior = dao.buscar(id)
            check(anterior == null || anterior.cuenta == cuenta && anterior.servidor == servidor) {
                "Este envío pertenece a otra cuenta o servidor."
            }
            if (anterior?.estado == EstadoEnvio.PENDIENTE.name) {
                check(anterior.formulario == formulario) { "Espera a que termine el envío anterior." }
                return@withTransaction anterior
            }
            val nuevo = EnvioFaena(id, cuenta, servidor, UUID.randomUUID().toString(), formulario)
            dao.guardar(nuevo)
            for (ruta in rutas) {
                val bytes = EvidenciasFotos.leer(contexto, Uri.parse(ruta))
                for (inicio in bytes.indices step TAMANO_FRAGMENTO) {
                    val fragmento = bytes.copyOfRange(inicio, minOf(inicio + TAMANO_FRAGMENTO, bytes.size))
                    dao.guardarFragmento(FragmentoFoto(id, ruta, inicio / TAMANO_FRAGMENTO, fragmento))
                }
            }
            nuevo
        }
        programar(envio)
    }

    suspend fun recuperarPendientes(cuenta: String) {
        dao.pendientes(cuenta, servidor).filter { it.estado == EstadoEnvio.PENDIENTE.name }.forEach(::programar)
    }

    suspend fun reintentar(cuenta: String, id: String) {
        val envio = dao.buscar(id)?.takeIf { it.cuenta == cuenta && it.servidor == servidor } ?: return
        if (envio.estado == EstadoEnvio.SINCRONIZADO.name) return
        if (dao.actualizar(id, envio.operacion, EstadoEnvio.PENDIENTE.name) == 1) programar(envio)
    }

    private fun programar(envio: EnvioFaena) {
        val solicitud = OneTimeWorkRequestBuilder<EnviarFaenaWorker>()
            .setInputData(workDataOf("faena" to envio.faena, "operacion" to envio.operacion))
            .addTag(ENVIO_FAENA)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(contexto).enqueueUniqueWork("faena-${envio.operacion}", ExistingWorkPolicy.KEEP, solicitud)
    }

    companion object {
        private const val TAMANO_FRAGMENTO = 256 * 1024
        private const val ENVIO_FAENA = "envio-expediente"
    }
}

class RecuperarEnviosWorker(contexto: Context, parametros: WorkerParameters) : CoroutineWorker(contexto, parametros) {
    override suspend fun doWork(): Result {
        val sesion = CredencialesSincronizacion.leer(applicationContext) ?: return Result.success()
        SincronizacionFaenas(applicationContext, sesion.servidor).recuperarPendientes(sesion.cuenta)
        return Result.success()
    }
}

class EnviarFaenaWorker(contexto: Context, parametros: WorkerParameters) : CoroutineWorker(contexto, parametros) {
    override suspend fun doWork(): Result {
        val id = inputData.getString("faena") ?: return Result.failure()
        val operacion = inputData.getString("operacion") ?: return Result.failure()
        val room = AquaFlowDatabase.obtener(applicationContext)
        val dao = room.sincronizacionDao()
        val envio = dao.buscar(id)?.takeIf { it.operacion == operacion && it.estado == EstadoEnvio.PENDIENTE.name }
            ?: return Result.success()
        val sesion = CredencialesSincronizacion.leer(applicationContext)
            ?.takeIf { it.cuenta == envio.cuenta && it.servidor == envio.servidor } ?: return Result.retry()
        val remoto = FaenasRemotas.desdeSesion(sesion)
        return try {
            val resumen = Gson().fromJson(envio.formulario, ResumenFaena::class.java)
            remoto.enviarExpediente(resumen, operacion) { ruta ->
                val cantidad = dao.cantidadFragmentos(id, ruta)
                check(cantidad > 0) { "Falta una fotografía en el envío local." }
                ByteArrayOutputStream().use { salida ->
                    repeat(cantidad) { salida.write(dao.fragmento(id, ruta, it)) }
                    salida.toByteArray()
                }
            }
            room.withTransaction {
                if (dao.actualizar(id, operacion, EstadoEnvio.SINCRONIZADO.name) == 1) dao.liberarFotos(id)
            }
            Result.success()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            val codigo = (error.cause as? HttpException)?.code()
            val transitorio = error.cause is IOException || codigo == 401 || codigo == 429 || (codigo ?: 0) >= 500
            dao.actualizar(id, operacion, if (transitorio) EstadoEnvio.PENDIENTE.name else EstadoEnvio.ERROR.name,
                error.message ?: "No se pudo sincronizar la faena.")
            if (transitorio) Result.retry() else Result.failure()
        }
    }
}
