package com.duoc.aquaflow.domain

import com.duoc.aquaflow.TestData
import com.duoc.aquaflow.domain.config.ReglasConfig
import com.duoc.aquaflow.domain.rules.Validadores
import com.duoc.aquaflow.model.Buzo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ValidadoresTest {
    private val zona = ZoneId.of("UTC")
    private val ahora = TestData.AHORA
    private val hoy = LocalDate.ofEpochDay(ahora / 86_400_000L)

    private fun buzo(vigenteHasta: LocalDate) = Buzo(
        id = 1, nombre = "Héctor", rut = "16162069-6", matricula = "1", categoria = "X",
        certificacionVigenteHastaEpochDay = vigenteHasta.toEpochDay()
    )

    @Test
    fun `apertura valida no tiene errores`() {
        val errores = Validadores.apertura(1L, buzo(hoy.plusYears(1)), ahora + 3_600_000, ahora, zona)
        assertTrue(errores.toString(), errores.isEmpty())
    }

    @Test
    fun `apertura exige centro y buzo`() {
        val errores = Validadores.apertura(null, null, ahora, ahora, zona)
        assertTrue(errores.containsKey("centro"))
        assertTrue(errores.containsKey("buzo"))
    }

    @Test
    fun `apertura rechaza fechas pasadas`() {
        val ayer = ahora - 2 * 86_400_000L
        assertTrue(Validadores.apertura(1L, buzo(hoy.plusYears(1)), ayer, ahora, zona).containsKey("fecha"))
    }

    @Test
    fun `apertura rechaza buzos con certificacion vencida a la fecha de la faena`() {
        val errores = Validadores.apertura(1L, buzo(hoy.minusDays(1)), ahora, ahora, zona)
        assertTrue(errores["buzo"]!!.contains("venció"))
        // El mismo buzo sirve si la certificación vence después de la faena.
        assertTrue(Validadores.apertura(1L, buzo(hoy), ahora, ahora, zona).isEmpty())
    }

    @Test
    fun `AST valida la profundidad contra el limite configurado`() {
        val config = ReglasConfig(profundidadMaxPlanificadaM = 30.0)
        assertTrue(Validadores.ast(TestData.ast().copy(profundidadMaxPlanificadaM = 30.0), config).isEmpty())
        assertTrue(Validadores.ast(TestData.ast().copy(profundidadMaxPlanificadaM = 30.1), config).containsKey("profundidad"))
        assertTrue(Validadores.ast(TestData.ast().copy(profundidadMaxPlanificadaM = 0.0), config).containsKey("profundidad"))
        assertTrue(Validadores.ast(TestData.ast().copy(profundidadMaxPlanificadaM = null), config).isEmpty())
    }

    @Test
    fun `un borrador de AST incompleto es valido`() {
        val borrador = TestData.ast().copy(tipologia = "", tarea = "", pasos = "")
        assertTrue(Validadores.ast(borrador).isEmpty())
    }

    @Test
    fun `encuesta de salud detecta valores imposibles`() {
        assertTrue(Validadores.encuestaSalud(TestData.encuesta()).isEmpty())
        assertTrue(Validadores.encuestaSalud(TestData.encuesta(sistolica = 400)).containsKey("sistolica"))
        assertTrue(Validadores.encuestaSalud(TestData.encuesta(spo2 = 120)).containsKey("spo2"))
        assertTrue(Validadores.encuestaSalud(TestData.encuesta(temperatura = 50.0)).containsKey("temperatura"))
    }

    @Test
    fun `la diastolica debe ser menor que la sistolica`() {
        assertTrue(Validadores.encuestaSalud(TestData.encuesta(sistolica = 80, diastolica = 90)).containsKey("diastolica"))
    }

    @Test
    fun `encuesta parcial es valida mientras los datos presentes sean posibles`() {
        val parcial = TestData.encuesta(sistolica = null, diastolica = null, spo2 = 97, temperatura = null)
        assertTrue(Validadores.encuestaSalud(parcial).isEmpty())
    }

    @Test
    fun `registro posterior exige todos los parametros dentro de rango`() {
        assertTrue(Validadores.registroPosterior(38, 24.6, 110).isEmpty())
        val vacios = Validadores.registroPosterior(null, null, null)
        assertEquals(setOf("tiempo", "profundidad", "presion"), vacios.keys)
        assertTrue(Validadores.registroPosterior(0, 10.0, 100).containsKey("tiempo"))
        assertTrue(Validadores.registroPosterior(30, -1.0, 100).containsKey("profundidad"))
        assertTrue(Validadores.registroPosterior(30, 10.0, 9999).containsKey("presion"))
    }

    @Test
    fun `el motivo de rechazo exige un largo minimo`() {
        val config = ReglasConfig(largoMinimoMotivo = 10)
        assertTrue(Validadores.motivo("corto", config).containsKey("motivo"))
        assertTrue(Validadores.motivo("   x   ", config).containsKey("motivo"))
        assertTrue(Validadores.motivo("Equipo de rescate sin presión", config).isEmpty())
    }

    @Test
    fun `nuevo usuario valida nombre RUT y PIN`() {
        assertTrue(Validadores.nuevoUsuario("Ana Pérez", "15892403-K", "4321", "4321").isEmpty())
        assertTrue(Validadores.nuevoUsuario("A", "15892403-K", "4321", "4321").containsKey("nombre"))
        assertTrue(Validadores.nuevoUsuario("Ana Pérez", "12345678-9", "4321", "4321").containsKey("rut"))
        assertTrue(Validadores.nuevoUsuario("Ana Pérez", "15892403-K", "43", "43").containsKey("pin"))
        assertTrue(Validadores.nuevoUsuario("Ana Pérez", "15892403-K", "4321", "1234").containsKey("pinConfirmacion"))
    }

    @Test
    fun `el PIN son exactamente cuatro digitos`() {
        assertTrue(Validadores.pinValido("0123"))
        assertFalse(Validadores.pinValido("123"))
        assertFalse(Validadores.pinValido("12345"))
        assertFalse(Validadores.pinValido("12a4"))
    }
}
