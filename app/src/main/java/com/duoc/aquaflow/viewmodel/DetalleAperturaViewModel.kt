package com.duoc.aquaflow.viewmodel

import android.content.Context
import com.duoc.aquaflow.data.remote.DetalleRemoto
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duoc.aquaflow.data.repository.ConsultaAperturas
import com.duoc.aquaflow.data.remote.FaenasRemotas
import com.duoc.aquaflow.data.repository.CierreFaenas
import com.duoc.aquaflow.model.RegistroCierre
import com.duoc.aquaflow.data.repository.RegistroFaena
import com.duoc.aquaflow.model.FaenaGuardada
import com.duoc.aquaflow.model.PreChequeo
import com.duoc.aquaflow.model.RegistroDecision
import com.duoc.aquaflow.model.Usuario
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DetalleAperturaUiState(
    val cargando: Boolean = true,
    val apertura: PreChequeo? = null,
    val error: String? = null,
    // La faena completa (apertura + secciones + decisión). `apertura` es la misma de `faena.resumen`.
    val faena: FaenaGuardada? = null,
    // Fallo al guardar la decisión de la jefatura (la lectura tiene su propio `error`)
    val errorDecision: String? = null,
    val guardando: Boolean = false,
    val cierrePublicado: Boolean = false,
    val expedienteRemoto: DetalleRemoto? = null
)

// `registro` permite que la jefatura guarde su decisión desde el detalle; con null solo se consulta.
@OptIn(ExperimentalCoroutinesApi::class)
class DetalleAperturaViewModel(
    consulta: ConsultaAperturas,
    private val usuario: Usuario,
    private val id: String,
    private val registro: RegistroFaena? = null,
    private val cierres: CierreFaenas? = null,
    private val remoto: FaenasRemotas? = null,
    private val contexto: Context? = null
) : ViewModel() {
    private val intentos = MutableStateFlow(0)
    private val errorDecision = MutableStateFlow<String?>(null)
    private val guardando = MutableStateFlow(false)

    private val datos = intentos.flatMapLatest {
        consulta.observarFaena(id, usuario)
            .map { local ->
                var errorDeLectura: String? = null
                val expediente = if (remoto != null) {
                    try {
                        remoto.detalle(requireNotNull(contexto), id)
                    } catch (error: IllegalStateException) {
                        if (local == null || usuario.rol != com.duoc.aquaflow.model.Rol.SUPERVISOR) throw error
                        if ((error.cause as? retrofit2.HttpException)?.code() != 404) errorDeLectura = error.message
                        null
                    }
                } else null
                val faena = if (expediente?.version == null && local != null && usuario.rol == com.duoc.aquaflow.model.Rol.SUPERVISOR)
                    local else expediente?.faena ?: local
                DetalleAperturaUiState(cargando = false, apertura = faena?.resumen?.apertura, faena = faena,
                    expedienteRemoto = expediente, cierrePublicado = expediente?.faena?.cierre != null, error = errorDeLectura)
            }
            .onStart { emit(DetalleAperturaUiState()) }
            .catch { emit(DetalleAperturaUiState(cargando = false, error = it.message ?: "No se pudo cargar la apertura.")) }
    }

    val state = combine(datos, errorDecision, guardando) { d, e, g ->
        d.copy(errorDecision = e, guardando = g)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalleAperturaUiState())

    fun reintentar() { intentos.update { it + 1 } }

    // Room vuelve a emitir al guardar, así la pantalla pasa sola a mostrar la decisión tomada.
    fun decidir(decision: RegistroDecision) {
        if (remoto != null) {
            val version = state.value.expedienteRemoto?.version ?: return
            operarRemoto { remoto.decidir(id, version, decision) }
            return
        }
        guardarDecision { registro -> registro.decidir(id, decision, usuario) }
    }

    fun auditarAutorizacion(motivo: String) {
        if (remoto != null) {
            decidir(RegistroDecision(com.duoc.aquaflow.model.Decision.AUTORIZADA, motivo, usuario.nombre))
            return
        }
        guardarDecision { registro -> registro.auditarAutorizacion(id, motivo, usuario) }
    }

    private fun guardarDecision(guardar: suspend (RegistroFaena) -> Unit) {
        val r = registro ?: return
        if (guardando.value) return
        if (usuario.rol != com.duoc.aquaflow.model.Rol.JEFE) {
            errorDecision.value = "Solo la jefatura puede decidir."
            return
        }
        guardando.value = true
        errorDecision.value = null
        viewModelScope.launch {
            try {
                guardar(r)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorDecision.value = "No se pudo guardar la decisión. ${e.localizedMessage.orEmpty()}"
            } finally {
                guardando.value = false
            }
        }
    }
    fun iniciar() {
        if (remoto != null) {
            operarRemoto { remoto.iniciar(id) }
            return
        }
        val servicio = cierres ?: return
        if (guardando.value) return
        guardando.value = true
        errorDecision.value = null
        viewModelScope.launch {
            try { servicio.iniciar(id, usuario) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { errorDecision.value = "No se pudo iniciar la faena. ${e.localizedMessage.orEmpty()}" }
            finally { guardando.value = false }
        }
    }

    fun cerrar(cierre: RegistroCierre) {
        if (remoto != null) {
            val solicitud = cierre.copy(cerradoEn = System.currentTimeMillis())
            operarRemoto { remoto.cerrar(id, solicitud) }
            return
        }
        val servicio = cierres ?: return
        if (guardando.value) return
        guardando.value = true
        errorDecision.value = null
        viewModelScope.launch {
            try {
                servicio.cerrar(id, cierre, usuario)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorDecision.value = "No se pudo guardar el cierre. ${e.localizedMessage.orEmpty()}"
            } finally {
                guardando.value = false
            }
        }
    }

    fun retirar() {
        val servicio = remoto ?: return
        val version = state.value.expedienteRemoto?.version ?: return
        operarRemoto { servicio.retirar(id, version) }
    }

    fun prepararEdicion(
        repositorio: com.duoc.aquaflow.data.repository.PreChequeoRepository,
        sincronizado: Boolean = false,
        abrir: () -> Unit
    ) {
        val faena = state.value.faena ?: return
        if (faena.decision != null || faena.cierre != null) return
        operarRemoto {
            val version = state.value.expedienteRemoto?.version
            if (version != null) remoto?.reservarEdicion(id, version)
            else check(!sincronizado || state.value.error == null) {
                "Conéctate para abrir una revisión del expediente sincronizado."
            }
            if (repositorio.recuperarFaenaPorId(id) == null) {
                val resumen = faena.resumen
                repositorio.guardar(resumen.apertura)
                resumen.checklist?.let { repositorio.guardarChecklist(id, it) }
                resumen.ast?.let { repositorio.guardarAst(id, it) }
                resumen.encuesta?.let { repositorio.guardarEncuesta(id, it) }
                resumen.tareas?.let { repositorio.guardarTareas(id, it) }
            }
            abrir()
        }
    }

    fun revisarAlerta(tipo: String) {
        val servicio = remoto ?: return
        operarRemoto { servicio.revisarAlerta(id, tipo) }
    }

    private fun operarRemoto(operacion: suspend () -> Unit) {
        if (guardando.value) return
        guardando.value = true
        errorDecision.value = null
        viewModelScope.launch {
            try {
                operacion()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorDecision.value = e.message ?: "No se pudo completar la operación."
            } finally {
                reintentar()
                guardando.value = false
            }
        }
    }

}
