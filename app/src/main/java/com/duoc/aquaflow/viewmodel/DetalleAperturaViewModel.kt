package com.duoc.aquaflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duoc.aquaflow.data.repository.ConsultaAperturas
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
    val guardando: Boolean = false
)

// `registro` permite que la jefatura guarde su decisión desde el detalle; con null solo se consulta.
@OptIn(ExperimentalCoroutinesApi::class)
class DetalleAperturaViewModel(
    consulta: ConsultaAperturas,
    private val usuario: Usuario,
    private val id: String,
    private val registro: RegistroFaena? = null
) : ViewModel() {
    private val intentos = MutableStateFlow(0)
    private val errorDecision = MutableStateFlow<String?>(null)
    private val guardando = MutableStateFlow(false)

    private val datos = intentos.flatMapLatest {
        consulta.observarFaena(id, usuario)
            .map { DetalleAperturaUiState(cargando = false, apertura = it?.resumen?.apertura, faena = it) }
            .onStart { emit(DetalleAperturaUiState()) }
            .catch { emit(DetalleAperturaUiState(cargando = false, error = "No se pudo cargar la apertura.")) }
    }

    val state = combine(datos, errorDecision, guardando) { d, e, g -> d.copy(errorDecision = e, guardando = g) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalleAperturaUiState())

    fun reintentar() { intentos.update { it + 1 } }

    // Room vuelve a emitir al guardar, así la pantalla pasa sola a mostrar la decisión tomada.
    fun decidir(decision: RegistroDecision) {
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
                r.decidir(id, decision, usuario)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorDecision.value = "No se pudo guardar la decisión. ${e.localizedMessage.orEmpty()}"
            } finally {
                guardando.value = false
            }
        }
    }
}
