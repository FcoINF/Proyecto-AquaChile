package com.duoc.aquaflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duoc.aquaflow.data.repository.ConsultaAperturas
import com.duoc.aquaflow.model.PreChequeo
import com.duoc.aquaflow.model.Usuario
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

data class DetalleAperturaUiState(
    val cargando: Boolean = true,
    val apertura: PreChequeo? = null,
    val error: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class DetalleAperturaViewModel(
    consulta: ConsultaAperturas,
    usuario: Usuario,
    id: String
) : ViewModel() {
    private val intentos = MutableStateFlow(0)
    val state = intentos.flatMapLatest {
        consulta.observarDetalle(id, usuario)
            .map { DetalleAperturaUiState(cargando = false, apertura = it) }
            .onStart { emit(DetalleAperturaUiState()) }
            .catch { emit(DetalleAperturaUiState(cargando = false, error = "No se pudo cargar la apertura.")) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalleAperturaUiState())

    fun reintentar() { intentos.update { it + 1 } }
}
