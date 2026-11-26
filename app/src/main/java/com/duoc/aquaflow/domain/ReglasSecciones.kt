package com.duoc.aquaflow.domain

import com.duoc.aquaflow.model.*

object ReglasSecciones {
    private fun estado(declarado: EstadoSeccion, valido: Boolean) =
        if (declarado == EstadoSeccion.COMPLETA && valido) EstadoSeccion.COMPLETA else EstadoSeccion.BORRADOR

    fun apertura(a: PreChequeo) =
        if (Reglas.errorApertura(a) == null) EstadoSeccion.COMPLETA else EstadoSeccion.BORRADOR

    const val FOTOS_OBLIGATORIAS_MINIMAS = 4

    fun checklistCompletable(c: ChecklistFaena): Boolean {
        val fotosAsociadas = c.fotosAsociadasPorEquipo()
        return c.items.isNotEmpty() &&
            c.items.none { it.estado == EstadoEquipo.PENDIENTE } &&
            c.items.all { fotosAsociadas[it.id].isNullOrBlank().not() }
    }

    fun checklistHabilitaContinuidad(c: ChecklistFaena) =
        checklistCompletable(c) && c.items.none { it.estado == EstadoEquipo.NO_CUMPLE }

    fun checklist(c: ChecklistFaena?) = c?.let {
        estado(it.estado, checklistCompletable(it))
    } ?: EstadoSeccion.SIN_REGISTRAR

    fun ast(a: FormularioAst?) = a?.let {
        estado(it.estado, ReglasAst.error(it.tipologia, it.respuestas, it.pasos) == null)
    } ?: EstadoSeccion.SIN_REGISTRAR

    fun encuesta(e: EncuestaSalud?) = e?.let {
        estado(
            it.estado,
            Reglas.enRango(it.sistolica, it.diastolica, it.saturacion) &&
                PREGUNTAS_SALUD.none { pregunta -> it.respuestas[pregunta] == null }
        )
    } ?: EstadoSeccion.SIN_REGISTRAR

    fun tareas(t: ChecklistTareas?) = t?.let {
        estado(it.estado, it.items.isNotEmpty() && it.items.all { item -> item.descripcion.isNotBlank() && item.verificada })
    } ?: EstadoSeccion.SIN_REGISTRAR

    fun estados(r: ResumenFaena): Map<String, EstadoSeccion> = linkedMapOf(
        "Apertura" to apertura(r.apertura),
        "Checklist" to (if (r.checklist != null && r.checklist.idFaena != r.apertura.id)
            EstadoSeccion.BORRADOR else checklist(r.checklist)),
        "AST" to ast(r.ast),
        "Encuesta de salud" to encuesta(r.encuesta),
        "Checklist de tareas" to tareas(r.tareas)
    )
}
