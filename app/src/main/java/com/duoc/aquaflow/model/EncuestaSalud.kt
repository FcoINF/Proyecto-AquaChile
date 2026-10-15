package com.duoc.aquaflow.model

// Nivel de los parámetros de salud (ficticios): alimenta el semáforo.
enum class NivelSalud(val texto: String) {
    NORMAL("Normal"),
    ATENCION("Atención"),
    CRITICO("Crítico")
}

// Encuesta preventiva: responder "Sí" a cualquiera genera una alerta.
val PREGUNTAS_SALUD = listOf(
    "¿Durmió menos de 6 horas?",
    "¿Presenta resfrío o dolor de oídos?",
    "¿Consumió alcohol en las últimas 24 horas?"
)

// Encuesta ya validada. Los nombres de campo coinciden con los que la guía agrega a PreChequeo
// (sistolica, diastolica, saturacion, alertasEncuesta), así luego se copian sin traducir.
data class EncuestaSalud(
    val sistolica: Int,
    val diastolica: Int,
    val saturacion: Int,
    val alertasEncuesta: List<String>,
    val nivel: NivelSalud
)