package com.duoc.aquaflow.domain.config

/**
 * Valores de referencia de signos vitales usados SOLO para alertar al supervisor/jefatura.
 *
 * ATENCIÓN: son valores PROVISIONALES de demostración. NO son criterios médicos oficiales ni los
 * del protocolo de AquaChile / Armada (los documentos del proyecto no los entregan). Deben ser
 * reemplazados por los valores validados por el cliente. La decisión de aptitud es siempre humana.
 */
data class ReferenciasSignosVitales(
    val sistolica: IntRange = 90..139,
    val diastolica: IntRange = 60..89,
    val saturacionMinima: Int = 95,
    val temperaturaMin: Double = 35.5,
    val temperaturaMax: Double = 37.5
)

/** Límites de plausibilidad: solo detectan errores de digitación, no son criterios clínicos. */
data class LimitesDigitacion(
    val sistolica: IntRange = 50..260,
    val diastolica: IntRange = 30..160,
    val saturacionO2: IntRange = 50..100,
    val temperaturaMin: Double = 30.0,
    val temperaturaMax: Double = 43.0,
    val tiempoInmersionMaxMin: Int = 600,
    val profundidadMaxM: Double = 100.0,
    val presionBancoMaxBar: Int = 400
)

/**
 * Configuración central de reglas operacionales. Cambiar un valor aquí no requiere tocar pantallas.
 * Los valores marcados como provisionales provienen de las maquetas y deben validarse con el cliente.
 */
data class ReglasConfig(
    val referencias: ReferenciasSignosVitales = ReferenciasSignosVitales(),
    val limites: LimitesDigitacion = LimitesDigitacion(),
    /** Provisional (maqueta AST: "Límite 30m"). */
    val profundidadMaxPlanificadaM: Double = 30.0,
    /** Si es true, signos vitales fuera de referencia impiden autorizar. Por defecto solo alertan. */
    val bloquearAutorizacionPorSignos: Boolean = false,
    /** Si es true, un equipo "No cumple"/"Observado" exige al menos una foto. */
    val exigirFotoEnFallas: Boolean = false,
    val largoMinimoMotivo: Int = 10,
    val maxIntentosLogin: Int = 5,
    val bloqueoLoginSegundos: Long = 30
)
