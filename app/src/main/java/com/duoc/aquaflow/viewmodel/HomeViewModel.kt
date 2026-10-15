package com.duoc.aquaflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duoc.aquaflow.model.EstadoFaena
import com.duoc.aquaflow.model.Faena
import com.duoc.aquaflow.model.Usuario
import com.duoc.aquaflow.repository.FaenaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val cargando: Boolean = true,
    /** Faenas del usuario que todavía no terminan su ciclo. */
    val activas: List<Faena> = emptyList(),
    /** Faenas esperando decisión de la jefatura. */
    val pendientesAutorizacion: List<Faena> = emptyList(),
    val ultima: Faena? = null,
    val totalPorEstado: Map<EstadoFaena, Int> = emptyMap(),
    val pendientesSync: Int = 0
)

class HomeViewModel(actor: Usuario, faenas: FaenaRepository) : ViewModel() {

    val state: StateFlow<HomeUiState> = combine(
        faenas.observarFaenas(actor),
        faenas.observarPendientesSync()
    ) { lista, pendientesSync ->
        HomeUiState(
            cargando = false,
            activas = lista.filter { it.estado.estaActiva },
            pendientesAutorizacion = lista.filter { it.estado == EstadoFaena.PENDIENTE_AUTORIZACION },
            ultima = lista.firstOrNull { it.estado.esTerminal },
            totalPorEstado = lista.groupingBy { it.estado }.eachCount(),
            pendientesSync = pendientesSync
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
