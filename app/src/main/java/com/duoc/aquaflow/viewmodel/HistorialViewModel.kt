package com.duoc.aquaflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duoc.aquaflow.data.repository.ConsultaAperturas
import com.duoc.aquaflow.domain.HistorialFiltro
import com.duoc.aquaflow.model.EstadoFaena
import com.duoc.aquaflow.model.HistorialItem
import com.duoc.aquaflow.model.Usuario
import com.duoc.aquaflow.model.aHistorialItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

data class HistorialUiState(
    val cargando: Boolean = true,
    val error: String? = null,
    val total: Int = 0,
    val filtro: HistorialFiltro = HistorialFiltro(),
    val faenas: List<HistorialItem> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class HistorialViewModel(consulta: ConsultaAperturas, usuario: Usuario) : ViewModel() {
    private val filtro = MutableStateFlow(HistorialFiltro())
    private val intentos = MutableStateFlow(0)

    // Room emite de nuevo cuando cambia una apertura guardada.
    private val datos = intentos.flatMapLatest {
        consulta.observarFaenas(usuario).map { lista ->
            val items = lista.map { it.aHistorialItem() }
            HistorialUiState(cargando = false, total = items.size, faenas = items)
        }.onStart { emit(HistorialUiState()) }
            .catch { emit(HistorialUiState(cargando = false, error = "No se pudo cargar el historial.")) }
    }

    val state = combine(datos, filtro) { datos, filtro ->
        datos.copy(filtro = filtro, faenas = filtro.aplicar(datos.faenas))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistorialUiState())

    fun onFecha(fecha: String) = filtro.update { it.copy(fecha = fecha.take(10)) }
    fun onTexto(texto: String) = filtro.update { it.copy(texto = texto.take(60)) }
    fun onEstado(estado: EstadoFaena?) = filtro.update { it.copy(estado = estado) }
    fun limpiar() { filtro.value = HistorialFiltro() }
    fun reintentar() { intentos.update { it + 1 } }
}
