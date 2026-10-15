package com.duoc.aquaflow.domain.catalog

import com.duoc.aquaflow.model.TipoEvidencia

/**
 * Catálogos de los formularios. Los ids son estables porque se guardan junto a las respuestas.
 * Los textos provienen de las maquetas (Clase 2). Los riesgos 2 y 3 y las tipologías son
 * PROVISIONALES: la maqueta solo muestra el primero ("0/3 verificados") y deben validarse con el cliente.
 */
data class EquipoCatalogo(
    val id: String,
    val categoria: String,
    val nombre: String,
    val descripcion: String
)

data class PreguntaCatalogo(
    val id: String,
    val categoria: String,
    val texto: String,
    val etiquetaSi: String = "SÍ",
    val etiquetaNo: String = "NO"
)

data class RiesgoCatalogo(
    val id: String,
    val titulo: String,
    val severidad: String,
    val medidaControl: String
)

data class SlotEvidencia(val tipo: TipoEvidencia, val titulo: String, val descripcion: String)

object Catalogos {

    val equipos: List<EquipoCatalogo> = listOf(
        EquipoCatalogo(
            id = "EQ_COMPRESOR",
            categoria = "Suministro primario",
            nombre = "Compresor, purgas y filtros de carbón",
            descripcion = "Test de CO/CO2 con tubo colorimétrico. Purgas automáticas drenadas y banco presurizado."
        ),
        EquipoCatalogo(
            id = "EQ_TRAJE",
            categoria = "Personal",
            nombre = "Traje seco, arnés y casco KMB",
            descripcion = "Prueba de estanqueidad de cremallera estanca, diafragma buconasal y fonía bidireccional limpia."
        ),
        EquipoCatalogo(
            id = "EQ_BAILOUT",
            categoria = "Respaldo / rescate",
            nombre = "Botellón de rescate (Bail-out)",
            descripcion = "Presión verificada en manómetro análogo sumergible. Conmutación manual activa."
        ),
        EquipoCatalogo(
            id = "EQ_UMBILICAL",
            categoria = "Alimentación",
            nombre = "Manguera umbilical y conexiones",
            descripcion = "Revisión de la manguera protectora externa y de las conexiones neumáticas y de audio."
        )
    )

    val gatingAst: List<PreguntaCatalogo> = listOf(
        PreguntaCatalogo(
            id = "AST_G_AMBIENTE",
            categoria = "Condición ambiental",
            texto = "¿Condiciones de viento, oleaje y corriente aptas según escala Beaufort (< 3) en fiordo?",
            etiquetaSi = "SÍ (APTO)"
        ),
        PreguntaCatalogo(
            id = "AST_G_APOYO",
            categoria = "Apoyo en superficie",
            texto = "¿Buzo de emergencia en lancha listo, equipado al 100% y con motor en neutro continuo?",
            etiquetaSi = "SÍ (APTO)"
        ),
        PreguntaCatalogo(
            id = "AST_G_EMERGENCIA",
            categoria = "Respuesta a emergencias",
            texto = "¿Plan de contingencia validado y cámara hiperbárica base con canal VHF y telefonía satelital activa?",
            etiquetaSi = "SÍ (APTO)"
        )
    )

    val riesgosAst: List<RiesgoCatalogo> = listOf(
        RiesgoCatalogo(
            id = "AST_R_ATRAPAMIENTO",
            titulo = "Atrapamiento en cabos o paños de red",
            severidad = "ALTA",
            medidaControl = "Buzo provisto de doble cuchillo de zafado rápido con ambas manos. Se prohíbe soltar la " +
                "línea guía de fondeo sin aviso previo por consola de comunicaciones."
        ),
        RiesgoCatalogo(
            id = "AST_R_SUMINISTRO",
            titulo = "Pérdida del suministro de aire",
            severidad = "ALTA",
            medidaControl = "Verificar presión de banco y botellón de rescate (bail-out) antes de la inmersión. " +
                "Ante pérdida de suministro, conmutar al respaldo y ascender según procedimiento."
        ),
        RiesgoCatalogo(
            id = "AST_R_AMBIENTE",
            titulo = "Condiciones ambientales adversas (corriente y oleaje)",
            severidad = "MEDIA",
            medidaControl = "Monitorear las condiciones durante toda la faena y suspender la inmersión si " +
                "superan los límites operativos definidos para el centro."
        )
    )

    /** Tipologías de tarea sugeridas (provisionales). */
    val tipologiasAst: List<String> = listOf(
        "Reparación de red lobera / pecera",
        "Inspección de fondo y fondeos",
        "Mantención de jaula",
        "Retiro de mortalidad"
    )

    val declaracionSalud: List<PreguntaCatalogo> = listOf(
        PreguntaCatalogo(
            id = "SAL_APTO_FISICO",
            categoria = "Aptitud física",
            texto = "¿Apto físicamente hoy? (Sin dolor de oídos, congestión ni barotraumas)",
            etiquetaSi = "SÍ (APTO)"
        ),
        PreguntaCatalogo(
            id = "SAL_DESCANSO",
            categoria = "Descanso",
            texto = "¿Descanso adecuado y sin fatiga? (Mínimo 8 horas de sueño en pontón)",
            etiquetaSi = "SÍ (APTO)"
        ),
        PreguntaCatalogo(
            id = "SAL_SIN_SUSTANCIAS",
            categoria = "Sustancias",
            texto = "¿Libre de fármacos y alcohol en las últimas 24 horas?",
            etiquetaSi = "SÍ (LIMPIO)"
        )
    )

    val slotsEvidencia: List<SlotEvidencia> = listOf(
        SlotEvidencia(TipoEvidencia.BUZO_EQUIPADO, "Buzo equipado", "Foto del buzo con su equipo completo."),
        SlotEvidencia(TipoEvidencia.BUZO_EMERGENCIA, "Buzo de emergencia", "Foto del buzo de emergencia listo."),
        SlotEvidencia(TipoEvidencia.FILTROS, "Filtros", "Foto de los filtros del compresor.")
    )

    fun equipo(id: String): EquipoCatalogo? = equipos.firstOrNull { it.id == id }
    fun gating(id: String): PreguntaCatalogo? = gatingAst.firstOrNull { it.id == id }
    fun riesgo(id: String): RiesgoCatalogo? = riesgosAst.firstOrNull { it.id == id }
    fun preguntaSalud(id: String): PreguntaCatalogo? = declaracionSalud.firstOrNull { it.id == id }
}
