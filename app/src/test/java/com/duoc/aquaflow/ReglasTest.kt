package com.duoc.aquaflow

import com.duoc.aquaflow.domain.Reglas
import com.duoc.aquaflow.model.NivelSalud
import com.duoc.aquaflow.model.PREGUNTAS_SALUD
import com.duoc.aquaflow.viewmodel.EncuestaSaludViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ReglasTest {

    @Test fun valoresNormales_esNormal() =
        assertEquals(NivelSalud.NORMAL, Reglas.nivelSalud(120, 80, 98, 0))

    @Test fun saturacionBaja_esCritico() =
        assertEquals(NivelSalud.CRITICO, Reglas.nivelSalud(120, 80, 90, 0))

    @Test fun presionAlta_esCritico() {
        assertEquals(NivelSalud.CRITICO, Reglas.nivelSalud(160, 80, 98, 0))
        assertEquals(NivelSalud.CRITICO, Reglas.nivelSalud(120, 100, 98, 0))
    }

    @Test fun saturacionLimite_esAtencion() =
        assertEquals(NivelSalud.ATENCION, Reglas.nivelSalud(120, 80, 94, 0))

    @Test fun presionElevada_esAtencion() =
        assertEquals(NivelSalud.ATENCION, Reglas.nivelSalud(140, 85, 98, 0))

    @Test fun alertaEnEncuesta_esAtencion() =
        assertEquals(NivelSalud.ATENCION, Reglas.nivelSalud(120, 80, 98, 1))

    @Test fun critico_ganaSobreAtencion() =
        assertEquals(NivelSalud.CRITICO, Reglas.nivelSalud(120, 80, 90, 3))
}

class EncuestaSaludViewModelTest {

    private fun vmCon(sis: String, dia: String, sat: String) = EncuestaSaludViewModel().apply {
        cambiarSistolica(sis); cambiarDiastolica(dia); cambiarSaturacion(sat)
    }

    @Test fun vacio_noGuardaYPideCompletar() {
        val vm = EncuestaSaludViewModel()
        assertNull(vm.guardar())
        assertEquals("Completa presión y saturación", vm.ui.value.error)
    }

    @Test fun sinNumeros_noHaySemaforo() = assertNull(EncuestaSaludViewModel().ui.value.salud)

    @Test fun datosNormales_guardaConNivelNormal() {
        val encuesta = vmCon("120", "80", "98").guardar()
        assertNotNull(encuesta)
        assertEquals(NivelSalud.NORMAL, encuesta!!.nivel)
    }

    @Test fun unaAlerta_pasaAAtencionYSeRegistra() {
        val vm = vmCon("120", "80", "98")
        vm.alternarAlerta(PREGUNTAS_SALUD[0])
        val encuesta = vm.guardar()!!
        assertEquals(NivelSalud.ATENCION, encuesta.nivel)
        assertEquals(listOf(PREGUNTAS_SALUD[0]), encuesta.alertasEncuesta)
    }

    @Test fun responderSi_agregaLaAlertaDeLaPregunta() {
        val vm = vmCon("120", "80", "98")

        vm.responderPregunta(PREGUNTAS_SALUD[0], respuestaAfirmativa = true)

        assertEquals(setOf(PREGUNTAS_SALUD[0]), vm.ui.value.alertas)
        assertEquals(NivelSalud.ATENCION, vm.ui.value.salud)
    }

    @Test fun responderNo_quitaLaAlertaDeLaPregunta() {
        val vm = vmCon("120", "80", "98")
        vm.responderPregunta(PREGUNTAS_SALUD[0], respuestaAfirmativa = true)

        vm.responderPregunta(PREGUNTAS_SALUD[0], respuestaAfirmativa = false)

        assertEquals(emptySet<String>(), vm.ui.value.alertas)
        assertEquals(NivelSalud.NORMAL, vm.ui.value.salud)
    }

    @Test fun saturacionFueraDeRango_noGuarda() {
        val vm = vmCon("120", "80", "150")
        assertNull(vm.guardar())
        assertEquals("Valores de salud fuera de rango", vm.ui.value.error)
    }

    @Test fun soloAceptaDigitos() {
        val vm = EncuestaSaludViewModel()
        vm.cambiarSistolica("1a2b0")
        assertEquals("120", vm.ui.value.sistolica)
    }
}