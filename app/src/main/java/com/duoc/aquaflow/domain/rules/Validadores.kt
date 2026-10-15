package com.duoc.aquaflow.domain.rules

import com.duoc.aquaflow.domain.FieldErrors
import com.duoc.aquaflow.domain.Fechas
import com.duoc.aquaflow.domain.config.ReglasConfig
import com.duoc.aquaflow.model.AntecedentesAst
import com.duoc.aquaflow.model.Buzo
import com.duoc.aquaflow.model.EncuestaSalud
import java.time.LocalDate
import java.time.ZoneId

/** Validaciones puras de formularios. Cada función devuelve un mapa campo -> mensaje (vacío si es válido). */
object Validadores {

    fun pinValido(pin: String): Boolean = pin.length == 4 && pin.all { it.isDigit() }

    fun apertura(
        centroId: Long?,
        buzo: Buzo?,
        fechaInicio: Long,
        ahora: Long,
        zona: ZoneId = ZoneId.systemDefault()
    ): FieldErrors {
        val errores = mutableMapOf<String, String>()
        if (centroId == null) errores["centro"] = "Selecciona el centro de cultivo."
        if (buzo == null) errores["buzo"] = "Selecciona el buzo asignado."
        if (fechaInicio < Fechas.inicioDelDia(ahora, zona)) {
            errores["fecha"] = "La fecha de la faena no puede ser anterior a hoy."
        }
        if (buzo != null) {
            val vigenteHasta = LocalDate.ofEpochDay(buzo.certificacionVigenteHastaEpochDay)
            if (Fechas.aLocalDate(fechaInicio, zona).isAfter(vigenteHasta)) {
                errores["buzo"] = "La certificación de ${buzo.nombre} venció el ${Fechas.fecha(vigenteHasta)}."
            }
        }
        return errores
    }

    fun ast(antecedentes: AntecedentesAst, config: ReglasConfig = ReglasConfig()): FieldErrors {
        val errores = mutableMapOf<String, String>()
        val prof = antecedentes.profundidadMaxPlanificadaM
        if (prof != null) {
            if (prof <= 0.0) {
                errores["profundidad"] = "La profundidad debe ser mayor que 0."
            } else if (prof > config.profundidadMaxPlanificadaM) {
                errores["profundidad"] = "Supera el límite configurado de ${config.profundidadMaxPlanificadaM} m."
            }
        }
        if (antecedentes.tarea.length > 300) errores["tarea"] = "Máximo 300 caracteres."
        if (antecedentes.pasos.length > 2000) errores["pasos"] = "Máximo 2000 caracteres."
        return errores
    }

    /** Valida solo los datos presentes (los borradores pueden estar incompletos). */
    fun encuestaSalud(encuesta: EncuestaSalud, config: ReglasConfig = ReglasConfig()): FieldErrors {
        val errores = mutableMapOf<String, String>()
        val lim = config.limites
        val sis = encuesta.sistolica
        val dia = encuesta.diastolica
        val spo2 = encuesta.saturacionO2
        val temp = encuesta.temperaturaC

        if (sis != null && sis !in lim.sistolica) {
            errores["sistolica"] = "Valor fuera de lo posible (${lim.sistolica.first}-${lim.sistolica.last})."
        }
        if (dia != null && dia !in lim.diastolica) {
            errores["diastolica"] = "Valor fuera de lo posible (${lim.diastolica.first}-${lim.diastolica.last})."
        }
        if (sis != null && dia != null && !errores.containsKey("diastolica") && dia >= sis) {
            errores["diastolica"] = "La diastólica debe ser menor que la sistólica."
        }
        if (spo2 != null && spo2 !in lim.saturacionO2) {
            errores["spo2"] = "Valor fuera de lo posible (${lim.saturacionO2.first}-${lim.saturacionO2.last})."
        }
        if (temp != null && (temp < lim.temperaturaMin || temp > lim.temperaturaMax)) {
            errores["temperatura"] = "Valor fuera de lo posible (${lim.temperaturaMin}-${lim.temperaturaMax} °C)."
        }
        if (encuesta.observaciones.length > 500) errores["observaciones"] = "Máximo 500 caracteres."
        return errores
    }

    fun registroPosterior(
        tiempoMin: Int?,
        profundidadM: Double?,
        presionBar: Int?,
        config: ReglasConfig = ReglasConfig()
    ): FieldErrors {
        val errores = mutableMapOf<String, String>()
        val lim = config.limites
        if (tiempoMin == null) {
            errores["tiempo"] = "Ingresa el tiempo de inmersión."
        } else if (tiempoMin <= 0 || tiempoMin > lim.tiempoInmersionMaxMin) {
            errores["tiempo"] = "Debe estar entre 1 y ${lim.tiempoInmersionMaxMin} minutos."
        }
        if (profundidadM == null) {
            errores["profundidad"] = "Ingresa la profundidad máxima."
        } else if (profundidadM <= 0.0 || profundidadM > lim.profundidadMaxM) {
            errores["profundidad"] = "Debe estar entre 0,1 y ${lim.profundidadMaxM} m."
        }
        if (presionBar == null) {
            errores["presion"] = "Ingresa la presión del banco."
        } else if (presionBar < 0 || presionBar > lim.presionBancoMaxBar) {
            errores["presion"] = "Debe estar entre 0 y ${lim.presionBancoMaxBar} bar."
        }
        return errores
    }

    /** Motivo obligatorio al rechazar o cancelar una faena. */
    fun motivo(motivo: String, config: ReglasConfig = ReglasConfig()): FieldErrors =
        when {
            motivo.trim().length < config.largoMinimoMotivo ->
                mapOf("motivo" to "Describe el motivo (mínimo ${config.largoMinimoMotivo} caracteres).")
            motivo.length > 500 -> mapOf("motivo" to "Máximo 500 caracteres.")
            else -> emptyMap()
        }

    fun nuevoUsuario(nombre: String, rut: String, pin: String, pinConfirmacion: String): FieldErrors {
        val errores = mutableMapOf<String, String>()
        if (nombre.trim().length < 3) errores["nombre"] = "Ingresa el nombre completo."
        if (!Rut.esValido(rut)) errores["rut"] = "RUT inválido."
        if (!pinValido(pin)) {
            errores["pin"] = "El PIN debe tener exactamente 4 dígitos."
        } else if (pin != pinConfirmacion) {
            errores["pinConfirmacion"] = "Los PIN no coinciden."
        }
        return errores
    }
}
