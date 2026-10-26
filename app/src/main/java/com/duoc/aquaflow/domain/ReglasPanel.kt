package com.duoc.aquaflow.domain

import com.duoc.aquaflow.model.EstadoFaena
import com.duoc.aquaflow.model.FaenaGuardada
import com.duoc.aquaflow.model.Rol

enum class GrupoFaena(val titulo: String) {
    ABIERTAS("Abiertas"),
    EN_PROCESO("Autorizadas / en proceso"),
    RECHAZADAS("Rechazadas"),
    CERRADAS("Cerradas")
}

object ReglasPanel {
    // Una faena sin decisión solo pasa a revisión cuando sus formularios están completos.
    fun grupo(faena: FaenaGuardada): GrupoFaena? = when (faena.estado) {
        EstadoFaena.CERRADA -> GrupoFaena.CERRADAS
        EstadoFaena.RECHAZADA -> GrupoFaena.RECHAZADAS
        EstadoFaena.AUTORIZADA, EstadoFaena.EN_EJECUCION -> GrupoFaena.EN_PROCESO
        EstadoFaena.PRECHEQUEO -> if (ReglasDecision.faltantes(faena.resumen).isEmpty())
            GrupoFaena.ABIERTAS else null
    }

    fun visible(faena: FaenaGuardada, rol: Rol) = rol == Rol.SUPERVISOR || grupo(faena) != null

    fun agrupar(faenas: List<FaenaGuardada>): Map<GrupoFaena, List<FaenaGuardada>> =
        GrupoFaena.entries.associateWith { categoria -> faenas.filter { grupo(it) == categoria } }
}
