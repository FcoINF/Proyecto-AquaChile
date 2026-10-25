package com.duoc.aquaflow.viewmodel

import androidx.lifecycle.ViewModel
import com.duoc.aquaflow.domain.Reglas
import com.duoc.aquaflow.model.EncuestaSalud
import com.duoc.aquaflow.model.NivelSalud
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// Todo lo que muestra la encuesta en un solo objeto inmutable.
data class EncuestaSaludUiState(
    val sistolica: String = "",
    val diastolica: String = "",
    val saturacion: String = "",
    val alertas: Set<String> = emptySet(),   // preguntas respondidas "Sí"
    val respuestas: Map<String, Boolean> = emptyMap(),
    val error: String? = null
) {
    // Nivel de salud en vivo (null mientras falten números)
    val salud: NivelSalud?
        get() {
            val sis = sistolica.toIntOrNull() ?: return null
            val dia = diastolica.toIntOrNull() ?: return null
            val sat = saturacion.toIntOrNull() ?: return null
            return Reglas.nivelSalud(sis, dia, sat, alertas.size)
        }

    // Marcan el campo en rojo apenas el valor digitado queda fuera de rango
    val sistolicaFuera get() = fuera(sistolica, Reglas.RANGO_SISTOLICA)
    val diastolicaFuera get() = fuera(diastolica, Reglas.RANGO_DIASTOLICA)
    val saturacionFuera get() = fuera(saturacion, Reglas.RANGO_SATURACION)

    private fun fuera(texto: String, rango: IntRange) = texto.toIntOrNull()?.let { it !in rango } == true
}

// Maneja la encuesta: guarda el estado, valida y entrega la encuesta lista para el pre-chequeo.
class EncuestaSaludViewModel : ViewModel() {
    // `_ui` (privado, mutable) solo lo cambia el ViewModel; `ui` (público) lo observa la pantalla.
    private val _ui = MutableStateFlow(EncuestaSaludUiState())
    val ui: StateFlow<EncuestaSaludUiState> = _ui.asStateFlow()

    private var encuestaCargada = false

    // Carga una sola vez para no sobrescribir ediciones al rotar la pantalla.
    fun cargar(encuesta: EncuestaSalud) {
        if (encuestaCargada) return
        encuestaCargada = true
        _ui.value = EncuestaSaludUiState(
            sistolica = encuesta.sistolica.toString(),
            diastolica = encuesta.diastolica.toString(),
            saturacion = encuesta.saturacion.toString(),
            alertas = encuesta.alertasEncuesta.toSet(),
            respuestas = encuesta.alertasEncuesta.associateWith { true }
        )
    }

    fun cambiarSistolica(v: String) = _ui.update { it.copy(sistolica = v.soloNumeros(), error = null) }
    fun cambiarDiastolica(v: String) = _ui.update { it.copy(diastolica = v.soloNumeros(), error = null) }
    fun cambiarSaturacion(v: String) = _ui.update { it.copy(saturacion = v.soloNumeros(), error = null) }

    // Un "Sí" entra al conjunto de alertas; al desmarcarlo sale
    fun alternarAlerta(pregunta: String) = _ui.update {
        val afirmativa = pregunta !in it.alertas
        it.copy(
            alertas = if (afirmativa) it.alertas + pregunta else it.alertas - pregunta,
            respuestas = it.respuestas + (pregunta to afirmativa),
            error = null
        )
    }

    fun responderPregunta(pregunta: String, respuestaAfirmativa: Boolean) = _ui.update {
        val alertas = if (respuestaAfirmativa) it.alertas + pregunta else it.alertas - pregunta
        it.copy(
            alertas = alertas,
            respuestas = it.respuestas + (pregunta to respuestaAfirmativa),
            error = null
        )
    }

    // Devuelve la encuesta si todo es válido; si no, deja el mensaje en `ui.error` y devuelve null.
    fun guardar(): EncuestaSalud? {
        val s = _ui.value
        val sis = s.sistolica.toIntOrNull()
        val dia = s.diastolica.toIntOrNull()
        val sat = s.saturacion.toIntOrNull()

        // La primera validación que falle define el mensaje que ve el usuario
        val error = when {
            sis == null || dia == null || sat == null -> "Completa presión y saturación"
            !Reglas.enRango(sis, dia, sat) -> "Valores de salud fuera de rango"
            else -> null
        }
        if (error != null || sis == null || dia == null || sat == null) {
            _ui.update { it.copy(error = error) }
            return null
        }
        return EncuestaSalud(sis, dia, sat, s.alertas.toList(), Reglas.nivelSalud(sis, dia, sat, s.alertas.size))
    }

    fun borrador(): EncuestaSalud {
        val s = _ui.value
        val sis = s.sistolica.toIntOrNull() ?: 0
        val dia = s.diastolica.toIntOrNull() ?: 0
        val sat = s.saturacion.toIntOrNull() ?: 0
        return EncuestaSalud(sis, dia, sat, s.alertas.toList(), s.salud ?: NivelSalud.NORMAL)
    }
}

private fun String.soloNumeros() = filter { it.isDigit() }.take(3)
