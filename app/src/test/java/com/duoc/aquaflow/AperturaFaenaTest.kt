package com.duoc.aquaflow

import com.duoc.aquaflow.domain.Reglas
import com.duoc.aquaflow.model.EncuestaSalud
import com.duoc.aquaflow.model.NivelSalud
import com.duoc.aquaflow.model.PreChequeo
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.model.Usuario
import com.duoc.aquaflow.viewmodel.FaenaViewModel
import com.duoc.aquaflow.viewmodel.EncuestaSaludViewModel
import org.junit.Assert.*
import org.junit.Test

class AperturaFaenaTest {
    private val supervisor = Usuario("supervisor", "1234", Rol.SUPERVISOR)
    private val encuesta = EncuestaSalud(120, 80, 98, emptyList(), NivelSalud.NORMAL)
    private fun apertura(fecha: String = "20/10/2026", hora: String = "09:30") =
        PreChequeo("1", "Centro demo", fecha, hora, "Buzo demo", "supervisor", true, true, true)

    @Test fun exigeTodosLosCampos() {
        val valida = apertura()
        listOf(
            valida.copy(centroCultivo = " "),
            valida.copy(fecha = ""),
            valida.copy(hora = ""),
            valida.copy(buzoAsignado = " "),
            valida.copy(supervisorACargo = " ")
        ).forEach { assertNotNull(Reglas.errorApertura(it)) }
        assertNull(Reglas.errorApertura(valida))
    }

    @Test fun validaCalendarioYHora() {
        assertNull(Reglas.errorApertura(apertura("29/02/2024", "00:00")))
        assertNull(Reglas.errorApertura(apertura(hora = "23:59")))
        listOf("29/02/2025", "31/04/2026", "1/10/2026", "20/10/2026x").forEach {
            assertNotNull(Reglas.errorApertura(apertura(fecha = it)))
        }
        listOf("24:00", "12:60", "9:30", "12:30x").forEach {
            assertNotNull(Reglas.errorApertura(apertura(hora = it)))
        }
    }

    @Test fun noAvanzaConAperturaIncompleta() {
        val vm = FaenaViewModel(BorradoresMemoria())
        vm.iniciar(supervisor)
        assertFalse(vm.confirmarApertura())
        assertFalse(vm.registrarEncuesta(encuesta))
    }

    @Test fun conservaIdYSupervisorDeLaSesion() {
        val vm = FaenaViewModel(BorradoresMemoria())
        vm.iniciar(supervisor)
        val id = vm.ui.value.apertura.id
        vm.actualizar(apertura().copy(id = "otro", supervisorACargo = "Nuevo Supervisor"))
        assertEquals(id, vm.ui.value.apertura.id)
        assertEquals("supervisor", vm.ui.value.apertura.supervisorACargo)
    }

    @Test fun cambiarBuzoInvalidaEncuestaAnterior() {
        val vm = FaenaViewModel(BorradoresMemoria())
        vm.iniciar(supervisor)
        vm.actualizar(apertura())
        assertTrue(vm.confirmarApertura())
        assertTrue(vm.registrarEncuesta(encuesta))
        vm.actualizar(vm.ui.value.apertura.copy(buzoAsignado = "Otro buzo"))
        assertNull(vm.ui.value.encuesta)
        assertFalse(vm.ui.value.aperturaConfirmada)
        assertFalse(vm.registrarEncuesta(encuesta))
    }

    @Test fun administradorNoIniciaFaena() {
        val vm = FaenaViewModel(BorradoresMemoria())
        vm.iniciar(Usuario("jefe", "1234", Rol.JEFE))
        assertEquals("", vm.ui.value.apertura.id)
        assertFalse(vm.confirmarApertura())
    }

    @Test fun cargarEncuestaNoSobrescribeEdicionesAlRecrearPantalla() {
        val vm = EncuestaSaludViewModel()
        vm.cargar(encuesta)
        vm.cambiarSistolica("130")
        vm.cargar(encuesta)
        assertEquals("130", vm.ui.value.sistolica)
    }
}
