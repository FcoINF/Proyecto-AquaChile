package com.duoc.aquaflow.data

import com.duoc.aquaflow.model.EncuestaSalud
import com.duoc.aquaflow.model.EstadoFaena
import com.duoc.aquaflow.model.HistorialItem
import com.duoc.aquaflow.model.NivelSalud

/**
 * Datos de referencia conservados del prototipo.
 * El historial de la aplicación consulta Room y no utiliza esta lista.
 */
object HistorialDemo {
    val faenas = listOf(
        HistorialItem(
            id = "1",
            codigo = "F-2026-001",
            centroCultivo = "Centro Punta de Tralca",
            buzoAsignado = "Juan Pérez",
            supervisorACargo = "supervisor",
            fecha = "2026-10-15",
            hora = "08:30",
            estado = EstadoFaena.CERRADA,
            encuesta = EncuestaSalud(120, 80, 98, emptyList(), NivelSalud.NORMAL)
        ),
        HistorialItem(
            id = "2",
            codigo = "F-2026-002",
            centroCultivo = "Centro Caleta Abajo",
            buzoAsignado = "María González",
            supervisorACargo = "supervisor",
            fecha = "2026-10-14",
            hora = "09:15",
            estado = EstadoFaena.AUTORIZADA,
            encuesta = EncuestaSalud(138, 88, 96, emptyList(), NivelSalud.ATENCION)
        ),
        HistorialItem(
            id = "3",
            codigo = "F-2026-003",
            centroCultivo = "Centro Bahía Profunda",
            buzoAsignado = "Carlos Rojas",
            supervisorACargo = "jefe",
            fecha = "2026-10-12",
            hora = "07:45",
            estado = EstadoFaena.RECHAZADA,
            encuesta = EncuestaSalud(165, 102, 90, listOf("¿Consumió alcohol en las últimas 24 horas?"), NivelSalud.CRITICO)
        ),
        HistorialItem(
            id = "4",
            codigo = "F-2026-004",
            centroCultivo = "Centro Punta de Tralca",
            buzoAsignado = "Andrés Soto",
            supervisorACargo = "supervisor",
            fecha = "2026-10-10",
            hora = "10:00",
            estado = EstadoFaena.PRECHEQUEO,
            encuesta = null
        ),
        HistorialItem(
            id = "5",
            codigo = "F-2026-005",
            centroCultivo = "Centro Estero Norte",
            buzoAsignado = "Pedro Núñez",
            supervisorACargo = "jefe",
            fecha = "2026-10-08",
            hora = "08:00",
            estado = EstadoFaena.CERRADA,
            encuesta = EncuestaSalud(118, 78, 99, emptyList(), NivelSalud.NORMAL)
        ),
        HistorialItem(
            id = "6",
            codigo = "F-2026-006",
            centroCultivo = "Centro Caleta Abajo",
            buzoAsignado = "Luis Vega",
            supervisorACargo = "supervisor",
            fecha = "2026-10-05",
            hora = "09:30",
            estado = EstadoFaena.AUTORIZADA,
            encuesta = EncuestaSalud(122, 79, 97, emptyList(), NivelSalud.NORMAL)
        ),
        HistorialItem(
            id = "7",
            codigo = "F-2026-007",
            centroCultivo = "Centro Bahía Profunda",
            buzoAsignado = "Ricardo Fuentes",
            supervisorACargo = "jefe",
            fecha = "2026-10-02",
            hora = "07:30",
            estado = EstadoFaena.RECHAZADA,
            encuesta = null
        )
    )
}
