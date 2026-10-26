package com.duoc.aquaflow.domain

import com.duoc.aquaflow.model.*

object ReglasSecciones {
    private fun estado(declarado: EstadoSeccion, valido: Boolean) =
        if (declarado == EstadoSeccion.COMPLETA && valido) EstadoSeccion.COMPLETA else EstadoSeccion.BORRADOR

    fun apertura(a: PreChequeo) =
        if (Reglas.errorApertura(a) == null) EstadoSeccion.COMPLETA else EstadoSeccion.BORRADOR

    fun checklist(c: ChecklistFaena?) = c?.let {
        estado(it.estado, it.items.isNotEmpty() && it.items.none { item -> item.estado == EstadoEquipo.PENDIENTE })
    } ?: EstadoSeccion.SIN_REGISTRAR

    fun ast(a: FormularioAst?) = a?.let {
        estado(it.estado, ReglasAst.error(it.tipologia, it.respuestas, it.pasos) == null)
    } ?: EstadoSeccion.SIN_REGISTRAR

    fun encuesta(e: EncuestaSalud?) = e?.let {
        estado(it.estado, Reglas.enRango(it.sistolica, it.diastolica, it.saturacion))
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
