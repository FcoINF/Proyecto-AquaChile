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
        apertura.centroId.isBlank() -> "Selecciona un centro de cultivo válido"
        apertura.centroCultivo.isBlank() -> "Ingresa el centro de cultivo"
        !fechaValida(apertura.fecha) -> "Ingresa una fecha válida con formato dd/MM/aaaa"
        !Regex("([01][0-9]|2[0-3]):[0-5][0-9]").matches(apertura.hora) ->
            "Ingresa una hora válida con formato HH:mm"
        apertura.buzoId.isBlank() -> "Selecciona un buzo identificado"
        apertura.buzoAsignado.isBlank() -> "Ingresa el nombre del buzo"
        apertura.edadBuzo !in 18..80 -> "Ingresa una edad válida para el buzo"
        apertura.imcBuzo !in 10.0..60.0 -> "Ingresa un IMC válido para el buzo"
        apertura.examenPreocupacionalVigente != true ||
            !vigenteEn(apertura.vigenciaExamenPreocupacional, apertura.fecha) ->
            "El examen preocupacional debe estar vigente a la fecha de la faena"
        apertura.puntajeCalbuco !in 0..100 -> "Ingresa un puntaje Calbuco entre 0 y 100"
        apertura.edadBuzo > 45 && (apertura.testEsfuerzoVigente != true ||
            !vigenteEn(apertura.vigenciaTestEsfuerzo, apertura.fecha)) ->
            "Bloqueo de seguridad: los buzos mayores de 45 años requieren un test de esfuerzo vigente"
        apertura.testEsfuerzoVigente == true &&
            !vigenteEn(apertura.vigenciaTestEsfuerzo, apertura.fecha) ->
            "La vigencia del test de esfuerzo debe cubrir la fecha de la faena"
        apertura.buzoEmergenciaDisponible != true ->
            "Confirma que el buzo de emergencia está disponible y apto"
        apertura.supervisorACargo.isBlank() -> "Falta el supervisor a cargo"
        !apertura.condicionesListas || apertura.embarcacionLista != true || apertura.comunicacionesOperativas != true ->
            "Confirma la verificación meteorológica, la embarcación y las comunicaciones operativas"
        else -> null
    }

    fun bloqueoTestEsfuerzo(apertura: PreChequeo): String? =
        if (apertura.edadBuzo > 45 && (apertura.testEsfuerzoVigente != true ||
                !vigenteEn(apertura.vigenciaTestEsfuerzo, apertura.fecha))) {
            "Buzo de ${apertura.edadBuzo} años: se exige test de esfuerzo vigente para abrir la faena."
        } else null

    private fun vigenteEn(vigencia: String, fechaFaena: String): Boolean {
        val vence = parsearFecha(vigencia) ?: return false
        val faena = parsearFecha(fechaFaena) ?: return false
        return !vence.before(faena)
    }

    private fun fechaValida(texto: String): Boolean {
        if (!Regex("[0-9]{2}/[0-9]{2}/[0-9]{4}").matches(texto)) return false
        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).apply {
            isLenient = false
        }
        val posicion = ParsePosition(0)
        return formato.parse(texto, posicion) != null && posicion.index == texto.length
    }

    private fun parsearFecha(texto: String) = if (Regex("[0-9]{2}/[0-9]{2}/[0-9]{4}").matches(texto)) {
        SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).apply { isLenient = false }.parse(texto)
    } else null
}
