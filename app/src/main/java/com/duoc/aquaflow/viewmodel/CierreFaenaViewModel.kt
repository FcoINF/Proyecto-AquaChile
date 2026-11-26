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
    val alertas: List<String> = emptyList(),
    val fondo: String = "",
    val profundidad: String = "",
    val descenso: String = "",
    val ascenso: String = "",
    val origen: OrigenMedicion? = null
)

class CierreFaenaViewModel(private val saved: SavedStateHandle) : ViewModel() {
    private val _ui = MutableStateFlow(CierreUiState(
        saved["minutos"] ?: "", saved["observaciones"] ?: "", saved["revision"] ?: false,
        sistolica = saved["sistolica"] ?: "", diastolica = saved["diastolica"] ?: "",
        saturacion = saved["saturacion"] ?: "", alertas = saved.get<ArrayList<String>>("alertas").orEmpty(),
        fondo = saved["fondo"] ?: "", profundidad = saved["profundidad"] ?: "",
        descenso = saved["descenso"] ?: "", ascenso = saved["ascenso"] ?: "",
        origen = saved.get<String>("origen")?.let(OrigenMedicion::valueOf)
    ))
    val ui = _ui.asStateFlow()
    fun minutos(valor: String) = actualizar(_ui.value.copy(minutos = valor.take(10)))
    fun observaciones(valor: String) = actualizar(_ui.value.copy(observaciones = valor.take(1000)))
    fun revision(valor: Boolean) = actualizar(_ui.value.copy(revisionSalida = valor))
    fun fondo(valor: String) = actualizar(_ui.value.copy(fondo = valor.take(10)))
    fun profundidad(valor: String) = actualizar(_ui.value.copy(profundidad = valor.take(12)))
    fun descenso(valor: String) = actualizar(_ui.value.copy(descenso = valor.take(12)))
    fun ascenso(valor: String) = actualizar(_ui.value.copy(ascenso = valor.take(12)))
    fun origen(valor: OrigenMedicion) = actualizar(_ui.value.copy(origen = valor))
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
        saved["fondo"] = estado.fondo
        saved["profundidad"] = estado.profundidad
        saved["descenso"] = estado.descenso
        saved["ascenso"] = estado.ascenso
        saved["origen"] = estado.origen?.name
    }

    private fun String.aDecimal() = trim().replace(',', '.').toDoubleOrNull()

    fun validar(): RegistroCierre? {
        val estado = _ui.value
        val minutos = estado.minutos.trim().toIntOrNull()
        val sis = estado.sistolica.toIntOrNull()
        val dia = estado.diastolica.toIntOrNull()
        val sat = estado.saturacion.toIntOrNull()
        val fondo = estado.fondo.trim().toIntOrNull()
        val profundidad = estado.profundidad.aDecimal()
        val descenso = estado.descenso.aDecimal()
        val ascenso = estado.ascenso.aDecimal()
        val origen = estado.origen
        val mediciones = if (fondo != null && profundidad != null && descenso != null && ascenso != null && origen != null)
            MedicionesInmersion(fondo, profundidad, descenso, ascenso, origen) else null
        val error = ReglasCierre.errorCampos(minutos, estado.observaciones, estado.revisionSalida)
            ?: ReglasCierre.errorMediciones(mediciones, requireNotNull(minutos))
            ?: if (sis == null || dia == null || sat == null) "Completa la encuesta de salud de salida."
            else if (!Reglas.enRango(sis, dia, sat)) "Valores de salud de salida fuera de rango." else null
        _ui.value = estado.copy(error = error)
        if (error != null) return null
        return RegistroCierre(requireNotNull(minutos), estado.observaciones.trim(), estado.revisionSalida,
            saludSalida = EncuestaSalud(requireNotNull(sis), requireNotNull(dia), requireNotNull(sat), estado.alertas,
                Reglas.nivelSalud(sis, dia, sat, estado.alertas.size),
                respuestas = PREGUNTAS_SALUD.associateWith { it !in estado.alertas }),
            mediciones = mediciones)
    }
}
