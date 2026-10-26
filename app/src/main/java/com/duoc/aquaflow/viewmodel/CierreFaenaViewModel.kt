package com.duoc.aquaflow.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.duoc.aquaflow.domain.ReglasCierre
import com.duoc.aquaflow.model.*
import com.duoc.aquaflow.domain.Reglas
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CierreUiState(
    val minutos: String = "",
    val observaciones: String = "",
    val revisionSalida: Boolean = false,
    val error: String? = null,
    val sistolica: String = "",
    val diastolica: String = "",
    val saturacion: String = "",
    val alertas: List<String> = emptyList()
)

class CierreFaenaViewModel(private val saved: SavedStateHandle) : ViewModel() {
    private val _ui = MutableStateFlow(CierreUiState(
        saved["minutos"] ?: "", saved["observaciones"] ?: "", saved["revision"] ?: false,
        sistolica = saved["sistolica"] ?: "", diastolica = saved["diastolica"] ?: "",
        saturacion = saved["saturacion"] ?: "", alertas = saved.get<ArrayList<String>>("alertas").orEmpty()
    ))
    val ui = _ui.asStateFlow()
    fun minutos(valor: String) = actualizar(_ui.value.copy(minutos = valor.take(10)))
    fun observaciones(valor: String) = actualizar(_ui.value.copy(observaciones = valor.take(1000)))
    fun revision(valor: Boolean) = actualizar(_ui.value.copy(revisionSalida = valor))
    fun sistolica(v: String) = actualizar(_ui.value.copy(sistolica = v.filter(Char::isDigit).take(3)))
    fun diastolica(v: String) = actualizar(_ui.value.copy(diastolica = v.filter(Char::isDigit).take(3)))
    fun saturacion(v: String) = actualizar(_ui.value.copy(saturacion = v.filter(Char::isDigit).take(3)))
    fun alerta(p: String, valor: Boolean) = actualizar(_ui.value.copy(alertas =
        if (valor) (_ui.value.alertas + p).distinct() else _ui.value.alertas - p))
    private fun actualizar(estado: CierreUiState) {
        _ui.value = estado.copy(error = null)
        saved["minutos"] = estado.minutos
        saved["observaciones"] = estado.observaciones
        saved["revision"] = estado.revisionSalida
        saved["sistolica"] = estado.sistolica
        saved["diastolica"] = estado.diastolica
        saved["saturacion"] = estado.saturacion
        saved["alertas"] = ArrayList(estado.alertas)
    }
    fun validar(): RegistroCierre? {
        val estado = _ui.value
        val minutos = estado.minutos.trim().toIntOrNull()
        val sis = estado.sistolica.toIntOrNull()
        val dia = estado.diastolica.toIntOrNull()
        val sat = estado.saturacion.toIntOrNull()
        val error = ReglasCierre.errorCampos(minutos, estado.observaciones, estado.revisionSalida)
            ?: if (sis == null || dia == null || sat == null) "Completa la encuesta de salud de salida."
            else if (!Reglas.enRango(sis, dia, sat)) "Valores de salud de salida fuera de rango." else null
        _ui.value = estado.copy(error = error)
        if (error != null) return null
        return RegistroCierre(requireNotNull(minutos), estado.observaciones.trim(), estado.revisionSalida,
            saludSalida = EncuestaSalud(requireNotNull(sis), requireNotNull(dia), requireNotNull(sat), estado.alertas,
                Reglas.nivelSalud(sis, dia, sat, estado.alertas.size)))
    }
}
