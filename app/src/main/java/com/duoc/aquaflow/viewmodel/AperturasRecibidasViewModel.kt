package com.duoc.aquaflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duoc.aquaflow.data.remote.AperturaRemota
import com.duoc.aquaflow.data.remote.FaenasRemotas
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AperturasRecibidasUiState(
    val aperturas: List<AperturaRemota> = emptyList(),
    val cargando: Boolean = false,
    val error: String? = null
)

class AperturasRecibidasViewModel(private val remoto: FaenasRemotas) : ViewModel() {
    private val _state = MutableStateFlow(AperturasRecibidasUiState())
    val state = _state.asStateFlow()

    init {
        actualizar()
    }

    fun actualizar() {
        if (_state.value.cargando) return
        _state.value = _state.value.copy(cargando = true, error = null)
        viewModelScope.launch {
            try {
                _state.value = AperturasRecibidasUiState(aperturas = remoto.listar())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    cargando = false,
                    error = e.message ?: "No se pudieron consultar las aperturas."
                )
            }
        }
    }
}
