package com.duoc.aquaflow.model

data class RegistroCierre(
    val duracionMinutos: Int,
    val observaciones: String,
    val revisionSalida: Boolean,
    val cerradoPor: String = "",
    val cerradoEn: Long = 0L,
    val saludSalida: EncuestaSalud? = null,
    val mediciones: MedicionesInmersion? = null
)

enum class OrigenMedicion(val texto: String) {
    MANUAL("Registro manual"),
    CENSUS("Lectura transcrita de Census")
}

data class MedicionesInmersion(
    val tiempoFondoMinutos: Int,
    val profundidadMaximaMetros: Double,
    val descensoMaximo: Double,
    val ascensoMaximo: Double,
    val origen: OrigenMedicion
) {
    val alertas: List<String>
        get() = buildList {
            if (descensoMaximo > 24.0) add("Descenso de $descensoMaximo m/min (superó 24 m/min)")
            if (ascensoMaximo > 9.0) add("Ascenso de $ascensoMaximo m/min (superó 9 m/min)")
        }
}
