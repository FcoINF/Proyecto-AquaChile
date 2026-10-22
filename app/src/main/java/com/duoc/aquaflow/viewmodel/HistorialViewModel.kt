package com.duoc.aquaflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duoc.aquaflow.data.HistorialDemo
import com.duoc.aquaflow.domain.HistorialFiltro
import com.duoc.aquaflow.model.EstadoFaena
import com.duoc.aquaflow.model.HistorialItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class HistorialUiState(
    val total: Int = HistorialDemo.faenas.size,
    val autorizadas: Int = 0,
    val rechazadas: Int = 0,
    val cerradas: Int = 0,
    val filtro: HistorialFiltro = HistorialFiltro(),
    val faenas: List<HistorialItem> = emptyList()
)

class HistorialViewModel : ViewModel() {
    private val _filtro = MutableStateFlow(HistorialFiltro())

    val state: StateFlow<HistorialUiState> = _filtro
        .map { filtro ->
            HistorialUiState(
                total = HistorialDemo.faenas.size,
                autorizadas = HistorialDemo.faenas.count { it.estado == EstadoFaena.AUTORIZADA },
                rechazadas = HistorialDemo.faenas.count { it.estado == EstadoFaena.RECHAZADA },
                cerradas = HistorialDemo.faenas.count { it.estado == EstadoFaena.CERRADA },
                filtro = filtro,
                faenas = filtro.aplicar(HistorialDemo.faenas)
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistorialUiState())

    fun onTexto(texto: String) = _filtro.update { it.copy(texto = texto.take(60)) }

    fun onEstado(estado: EstadoFaena?) = _filtro.update { it.copy(estado = estado) }

    fun limpiar() = _filtro.update { HistorialFiltro() }
}
