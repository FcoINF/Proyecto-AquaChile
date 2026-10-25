package com.duoc.aquaflow.domain

import com.duoc.aquaflow.model.NivelSalud
import com.duoc.aquaflow.model.PreChequeo
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale

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


    // Devuelve el primer problema de la apertura; null significa válida.
    fun errorApertura(apertura: PreChequeo): String? = when {
        apertura.centroCultivo.isBlank() -> "Ingresa el centro de cultivo"
        !fechaValida(apertura.fecha) -> "Ingresa una fecha válida con formato dd/MM/aaaa"
        !Regex("([01][0-9]|2[0-3]):[0-5][0-9]").matches(apertura.hora) ->
            "Ingresa una hora válida con formato HH:mm"
        apertura.buzoAsignado.isBlank() -> "Ingresa el nombre del buzo"
        apertura.supervisorACargo.isBlank() -> "Falta el supervisor a cargo"
        !apertura.condicionesListas || apertura.embarcacionLista != true || apertura.comunicacionesOperativas != true ->
            "Debes confirmar todas las condiciones operativas iniciales"
        else -> null
    }

    private fun fechaValida(texto: String): Boolean {
        if (!Regex("[0-9]{2}/[0-9]{2}/[0-9]{4}").matches(texto)) return false
        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).apply {
            isLenient = false
        }
        val posicion = ParsePosition(0)
        return formato.parse(texto, posicion) != null && posicion.index == texto.length
    }
}
