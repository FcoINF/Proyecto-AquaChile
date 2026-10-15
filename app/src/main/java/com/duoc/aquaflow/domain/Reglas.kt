package com.duoc.aquaflow.domain

import com.duoc.aquaflow.model.NivelSalud

// Reglas de negocio puras (sin Android): fáciles de leer y de probar.
// Los umbrales son ficticios, solo para el ejercicio académico.
object Reglas {

    // Rangos aceptados al digitar: fuera de ellos se asume error de tipeo
    val RANGO_SISTOLICA = 70..250
    val RANGO_DIASTOLICA = 40..150
    val RANGO_SATURACION = 50..100

    // El primer `when` que se cumpla gana, por eso CRITICO va antes que ATENCION
    fun nivelSalud(sistolica: Int, diastolica: Int, saturacion: Int, alertas: Int) = when {
        saturacion < 92 || sistolica >= 160 || diastolica >= 100 -> NivelSalud.CRITICO
        saturacion < 95 || sistolica >= 140 || diastolica >= 90 || alertas > 0 -> NivelSalud.ATENCION
        else -> NivelSalud.NORMAL
    }

    fun enRango(sistolica: Int, diastolica: Int, saturacion: Int) =
        sistolica in RANGO_SISTOLICA && diastolica in RANGO_DIASTOLICA && saturacion in RANGO_SATURACION
}