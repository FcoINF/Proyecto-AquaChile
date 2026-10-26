package com.duoc.aquaflow

import com.duoc.aquaflow.model.*

internal fun tareasCompletas() = ChecklistTareas(listOf(TareaFaena("Inspección de red", true)))
internal fun saludSalida() = EncuestaSalud(125, 82, 97, listOf("¿Presenta resfrío o dolor de oídos?"), NivelSalud.ATENCION)
