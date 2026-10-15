package com.duoc.aquaflow.domain

import com.duoc.aquaflow.TestData
import com.duoc.aquaflow.domain.rules.FaenaFiltro
import com.duoc.aquaflow.domain.rules.RangoFecha
import com.duoc.aquaflow.model.EstadoFaena
import org.junit.Assert.assertEquals
import org.junit.Test

class FaenaFiltroTest {
    private val ahora = TestData.AHORA
    private val dia = 24L * 60 * 60 * 1000

    private val faenas = listOf(
        TestData.faena(id = 1, estado = EstadoFaena.CERRADA, fechaInicio = ahora - 40 * dia, buzoNombre = "Héctor Barría", centroNombre = "Centro Huito"),
        TestData.faena(id = 2, estado = EstadoFaena.AUTORIZADA, fechaInicio = ahora - 3 * dia, buzoNombre = "Matías Soto", centroNombre = "Centro Calbuco"),
        TestData.faena(id = 3, estado = EstadoFaena.RECHAZADA, fechaInicio = ahora - 10 * dia, buzoNombre = "Felipe Navarro", centroNombre = "Centro Huito")
    )

    @Test
    fun `sin filtros devuelve todo`() {
        assertEquals(3, FaenaFiltro().aplicar(faenas, ahora).size)
    }

    @Test
    fun `filtra por estado`() {
        val r = FaenaFiltro(estado = EstadoFaena.RECHAZADA).aplicar(faenas, ahora)
        assertEquals(listOf(3L), r.map { it.id })
    }

    @Test
    fun `busca por texto en buzo centro y codigo sin importar mayusculas`() {
        assertEquals(listOf(1L, 3L), FaenaFiltro(texto = "huito").aplicar(faenas, ahora).map { it.id })
        assertEquals(listOf(2L), FaenaFiltro(texto = "MATÍAS").aplicar(faenas, ahora).map { it.id })
        assertEquals(listOf(3L), FaenaFiltro(texto = "0003").aplicar(faenas, ahora).map { it.id })
    }

    @Test
    fun `filtra por rango de fechas`() {
        assertEquals(listOf(2L), FaenaFiltro(rango = RangoFecha.ULTIMOS_7).aplicar(faenas, ahora).map { it.id })
        assertEquals(listOf(2L, 3L), FaenaFiltro(rango = RangoFecha.ULTIMOS_30).aplicar(faenas, ahora).map { it.id })
    }

    @Test
    fun `combina filtros`() {
        val r = FaenaFiltro(texto = "huito", rango = RangoFecha.ULTIMOS_30).aplicar(faenas, ahora)
        assertEquals(listOf(3L), r.map { it.id })
    }
}
