package com.duoc.aquaflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duoc.aquaflow.data.repository.ConsultaAperturas
import com.duoc.aquaflow.model.FaenaGuardada
import com.duoc.aquaflow.model.Usuario
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

data class DashboardUiState(
    val cargando: Boolean = true,
    val error: String? = null,
    val faenas: List<FaenaGuardada> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(consulta: ConsultaAperturas, usuario: Usuario) : ViewModel() {
    private val intentos = MutableStateFlow(0)
    val state = intentos.flatMapLatest {
        consulta.observarFaenas(usuario)
            .map { DashboardUiState(cargando = false, faenas = it) }
            .onStart { emit(DashboardUiState()) }
            .catch { emit(DashboardUiState(cargando = false, error = "No se pudieron cargar las faenas.")) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun reintentar() { intentos.update { it + 1 } }
}
