package com.duoc.aquaflow

import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.model.Usuario
import com.duoc.aquaflow.viewmodel.FaenaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class BorradorAperturaTest {
    private val dispatcher = StandardTestDispatcher()
    private val datos = BorradoresMemoria()
    private val usuario = Usuario("supervisor", "1234", Rol.SUPERVISOR)
    @Before fun preparar() { Dispatchers.setMain(dispatcher) }
    @After fun terminar() { Dispatchers.resetMain() }

    @Test fun recuperaBorradorIncompletoEnOtroViewModelSinCambiarIdentidad() = runTest {
        val original = FaenaViewModel(datos).apply { iniciar(usuario) }
        original.actualizar(original.ui.value.apertura.copy(centroCultivo = "Centro demo"))
        original.guardarApertura()
        advanceUntilIdle()
        val nuevo = FaenaViewModel(datos).apply { iniciar(usuario) }
        nuevo.recuperarApertura()
        advanceUntilIdle()
        assertEquals(original.ui.value.apertura, nuevo.ui.value.apertura)
        assertFalse(nuevo.confirmarApertura())
        assertNotNull(nuevo.ui.value.error)
    }

    @Test fun falloAlGuardarNoNavegaYPermiteReintentar() = runTest {
        val vm = FaenaViewModel(datos).apply { iniciar(usuario) }
        vm.actualizar(vm.ui.value.apertura.copy(centroCultivo = "Centro", buzoAsignado = "Buzo"))
        var avances = 0
        datos.fallar = true
        vm.guardarApertura(continuar = true) { avances++ }
        advanceUntilIdle()
        assertEquals(0, avances)
        assertNotNull(vm.ui.value.error)
        assertFalse(vm.ui.value.ocupado)
        datos.fallar = false
        vm.guardarApertura(continuar = true) { avances++ }
        advanceUntilIdle()
        assertEquals(1, avances)
        assertNotNull(datos.apertura)
    }

    @Test fun noRecuperaAperturaDeOtroSupervisor() = runTest {
        val vm = FaenaViewModel(datos).apply { iniciar(usuario) }
        vm.guardarApertura()
        advanceUntilIdle()
        val otro = FaenaViewModel(datos).apply { iniciar(usuario.copy(nombre = "otro")) }
        val inicial = otro.ui.value.apertura
        otro.recuperarApertura()
        advanceUntilIdle()
        assertEquals(inicial, otro.ui.value.apertura)
        assertEquals("No tienes aperturas guardadas.", otro.ui.value.mensaje)
    }
}
