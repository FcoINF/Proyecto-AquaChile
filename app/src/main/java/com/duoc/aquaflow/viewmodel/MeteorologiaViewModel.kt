package com.duoc.aquaflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duoc.aquaflow.data.repository.MeteorologiaRepository
import com.duoc.aquaflow.data.repository.ResultadoMeteorologia
import com.duoc.aquaflow.model.CENTROS_MARITIMOS
import com.duoc.aquaflow.model.CentroMaritimo
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MeteorologiaUi(
    val centro: CentroMaritimo? = null, val cargando: Boolean = false, val resultado: ResultadoMeteorologia? = null
)

class MeteorologiaViewModel(private val repository: MeteorologiaRepository) : ViewModel() {
    private val _ui = MutableStateFlow(MeteorologiaUi())
    val ui = _ui.asStateFlow()
    private var consulta: Job? = null

    fun seleccionar(centroId: String) {
        val centro = CENTROS_MARITIMOS.find { it.id == centroId }
        if (_ui.value.centro != centro) cargar(centro)
    }

    fun actualizar() {
        val centro = _ui.value.centro ?: return
        if (!_ui.value.cargando) cargar(centro)
    }

    private fun cargar(centro: CentroMaritimo?) {
        consulta?.cancel()
        _ui.value = MeteorologiaUi(centro, cargando = centro != null)
        if (centro != null) consulta = viewModelScope.launch {
            _ui.value = MeteorologiaUi(centro, resultado = repository.consultar(centro))
        }
    }
}
