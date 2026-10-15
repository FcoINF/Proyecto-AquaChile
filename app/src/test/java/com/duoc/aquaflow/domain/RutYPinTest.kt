package com.duoc.aquaflow.domain

import com.duoc.aquaflow.domain.rules.PinHasher
import com.duoc.aquaflow.domain.rules.Rut
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RutYPinTest {

    @Test
    fun `normaliza el RUT sin puntos y con DV en mayuscula`() {
        assertEquals("15892403-K", Rut.normalizar("15.892.403-k"))
        assertEquals("12433910-3", Rut.normalizar(" 12433910 3 "))
    }

    @Test
    fun `rechaza formatos que no son RUT`() {
        assertNull(Rut.normalizar("abc"))
        assertNull(Rut.normalizar("1-9"))
        assertNull(Rut.normalizar(""))
    }

    @Test
    fun `valida el digito verificador con modulo 11`() {
        assertTrue(Rut.esValido("15.892.403-K"))
        assertTrue(Rut.esValido("16162069-6"))
        assertTrue(Rut.esValido("12433910-3"))
        assertFalse(Rut.esValido("12433910-K"))
        assertFalse(Rut.esValido("12345678-9"))
    }

    @Test
    fun `calcula el digito verificador`() {
        assertEquals('6', Rut.calcularDv("16162069"))
        assertEquals('K', Rut.calcularDv("15892403"))
        assertEquals('5', Rut.calcularDv("12345678"))
    }

    @Test
    fun `formatea con puntos y guion`() {
        assertEquals("15.892.403-K", Rut.formatear("15892403-K"))
        assertEquals("texto raro", Rut.formatear("texto raro"))
    }

    @Test
    fun `el hash del PIN verifica el PIN correcto y rechaza otros`() {
        val sal = PinHasher.nuevaSal()
        val hash = PinHasher.hash("4921", sal)
        assertTrue(PinHasher.verificar("4921", sal, hash))
        assertFalse(PinHasher.verificar("4922", sal, hash))
        assertFalse(hash.contains("4921"))
    }

    @Test
    fun `la misma clave con distinta sal produce hashes distintos`() {
        val a = PinHasher.hash("1234", PinHasher.nuevaSal())
        val b = PinHasher.hash("1234", PinHasher.nuevaSal())
        assertNotEquals(a, b)
    }
}
