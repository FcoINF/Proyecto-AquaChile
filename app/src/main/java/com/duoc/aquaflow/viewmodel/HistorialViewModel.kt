package com.duoc.aquaflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duoc.aquaflow.domain.rules.FaenaFiltro
import com.duoc.aquaflow.domain.rules.RangoFecha
import com.duoc.aquaflow.model.EstadoFaena
import com.duoc.aquaflow.model.Faena
import com.duoc.aquaflow.model.RegistroPosterior
import com.duoc.aquaflow.model.Usuario
import com.duoc.aquaflow.repository.FaenaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class HistorialUiState(
    val cargando: Boolean = true,
    val filtro: FaenaFiltro = FaenaFiltro(),
    val faenas: List<Faena> = emptyList(),
    val total: Int = 0,
    /** Registros posteriores indexados por faenaId (solo faenas cerradas). */
    val registros: Map<Long, RegistroPosterior> = emptyMap()
)

class HistorialViewModel(
    actor: Usuario,
    faenas: FaenaRepository,
    private val ahora: () -> Long = { System.currentTimeMillis() }
) : ViewModel() {

    private val filtro = MutableStateFlow(FaenaFiltro())

    val state: StateFlow<HistorialUiState> = combine(
        faenas.observarFaenas(actor),
        faenas.observarRegistros(),
        filtro
    ) { lista, registros, f ->
        HistorialUiState(
            cargando = false,
            filtro = f,
            faenas = f.aplicar(lista, ahora()),
            total = lista.size,
            registros = registros
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistorialUiState())

    fun onTexto(texto: String) = filtro.update { it.copy(texto = texto.take(60)) }

    fun onEstado(estado: EstadoFaena?) = filtro.update { it.copy(estado = estado) }

    fun onRango(rango: RangoFecha) = filtro.update { it.copy(rango = rango) }

    fun limpiar() = filtro.update { FaenaFiltro() }
}
