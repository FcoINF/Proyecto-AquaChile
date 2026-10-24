package com.duoc.aquaflow

import com.duoc.aquaflow.data.repository.ConsultaAperturas
import com.duoc.aquaflow.domain.HistorialFiltro
import com.duoc.aquaflow.model.*
import com.duoc.aquaflow.viewmodel.DetalleAperturaViewModel
import com.duoc.aquaflow.viewmodel.HistorialViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class HistorialRoomViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val usuario = Usuario("jefe", "1234", Rol.JEFE)
    private val primera = PreChequeo("uuid-1", "Centro Norte", "22/10/2026", "09:00", "Ana", "Luis")
    private val segunda = primera.copy(id = "uuid-2", centroCultivo = "Centro Sur", buzoAsignado = "Pedro")
    private val consulta = ConsultaFake()

    @Before fun preparar() { Dispatchers.setMain(dispatcher) }
    @After fun terminar() { Dispatchers.resetMain() }

    @Test fun actualizaListaYConservaFiltroAlCambiarLosDatos() = runTest {
        val vm = HistorialViewModel(consulta, usuario)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect() }
        consulta.lista.value = listOf(primera, segunda)
        advanceUntilIdle()
        assertEquals(2, vm.state.value.total)
        vm.onTexto("  NORTE  ")
        advanceUntilIdle()
        assertEquals(listOf("uuid-1"), vm.state.value.faenas.map { it.id })
        consulta.lista.value = listOf(primera.copy(centroCultivo = "Centro Este"), segunda)
        advanceUntilIdle()
        assertTrue(vm.state.value.faenas.isEmpty())
        assertEquals(2, vm.state.value.total)
        vm.limpiar()
        advanceUntilIdle()
        assertEquals(2, vm.state.value.faenas.size)
    }

    @Test fun filtrosBuscanTodosLosCamposYCombinanEstado() {
        val item = primera.aHistorialItem()
        listOf("uuid-1", "norte", "ANA", "luis").forEach {
            assertEquals(listOf(item), HistorialFiltro(it, EstadoFaena.PRECHEQUEO).aplicar(listOf(item)))
        }
        assertTrue(HistorialFiltro("Ana", EstadoFaena.AUTORIZADA).aplicar(listOf(item)).isEmpty())
    }

    @Test fun historialVacioNoInventaDatosDemo() = runTest {
        val vm = HistorialViewModel(consulta, usuario)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect() }
        advanceUntilIdle()
        assertFalse(vm.state.value.cargando)
        assertEquals(0, vm.state.value.total)
        assertTrue(vm.state.value.faenas.isEmpty())
        assertNull(vm.state.value.error)
    }

    @Test fun falloDeLecturaPuedeReintentarse() = runTest {
        consulta.fallar = true
        val vm = HistorialViewModel(consulta, usuario)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect() }
        advanceUntilIdle()
        assertNotNull(vm.state.value.error)
        assertFalse(vm.state.value.cargando)
        consulta.fallar = false
        consulta.lista.value = listOf(primera)
        vm.reintentar()
        advanceUntilIdle()
        assertNull(vm.state.value.error)
        assertEquals(1, vm.state.value.total)
    }

    @Test fun detalleConsultaElIdCorrectoYReflejaActualizaciones() = runTest {
        consulta.lista.value = listOf(primera, segunda)
        val vm = DetalleAperturaViewModel(consulta, usuario, segunda.id)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect() }
        advanceUntilIdle()
        assertEquals(segunda, vm.state.value.apertura)
        val editada = segunda.copy(buzoAsignado = "Buzo corregido")
        consulta.lista.value = listOf(primera, editada)
        advanceUntilIdle()
        assertEquals(editada, vm.state.value.apertura)
        consulta.lista.value = listOf(primera)
        advanceUntilIdle()
        assertFalse(vm.state.value.cargando)
        assertNull(vm.state.value.apertura)
        assertNull(vm.state.value.error)
    }

    @Test fun detalleDistingueErrorDeRegistroAusenteYPermiteReintentar() = runTest {
        consulta.fallar = true
        val vm = DetalleAperturaViewModel(consulta, usuario, primera.id)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect() }
        advanceUntilIdle()
        assertNotNull(vm.state.value.error)
        consulta.fallar = false
        consulta.lista.value = listOf(primera)
        vm.reintentar()
        advanceUntilIdle()
        assertNull(vm.state.value.error)
        assertEquals(primera, vm.state.value.apertura)
    }

    private class ConsultaFake : ConsultaAperturas {
        val lista = MutableStateFlow<List<PreChequeo>>(emptyList())
        var fallar = false
        override fun observar(usuario: Usuario): Flow<List<PreChequeo>> = flow {
            check(!fallar)
            emitAll(lista)
        }
        override fun observarDetalle(id: String, usuario: Usuario) =
            observar(usuario).map { lista -> lista.find { it.id == id } }
    }
}
