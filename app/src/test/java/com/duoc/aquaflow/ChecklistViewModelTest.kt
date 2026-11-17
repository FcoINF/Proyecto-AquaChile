package com.duoc.aquaflow

import androidx.lifecycle.SavedStateHandle
import com.duoc.aquaflow.model.EstadoEquipo
import com.duoc.aquaflow.viewmodel.ChecklistViewModel
import org.junit.Assert.*
import org.junit.Test

class ChecklistViewModelTest {
    @Test fun conservaEstadosYCapturaPendienteAlRecrearElViewModel() {
        val saved = SavedStateHandle()
        val vm = ChecklistViewModel(saved)
        vm.cargar("f1", null)
        vm.ui.value.items.forEach { vm.cambiarEstado(it.id, EstadoEquipo.CUMPLE) }
        vm.prepararFoto("content://aquaflow.evidencias/foto1.jpg")
        val restaurado = ChecklistViewModel(saved)
        restaurado.cargar("f1", null)
        assertEquals(vm.ui.value, restaurado.ui.value)
        restaurado.terminarFoto(true)
        listOf("2", "3", "4").forEach {
            restaurado.prepararFoto("content://aquaflow.evidencias/foto$it.jpg")
            restaurado.terminarFoto(true)
        }
        assertEquals(4, restaurado.guardar()!!.fotosEvidencia.size)
        assertNull(restaurado.ui.value.pendiente)
    }

    @Test fun resultadoDeCamaraAntesDeComponerNoPierdeLaCapturaPendiente() {
        val saved = SavedStateHandle()
        val vm = ChecklistViewModel(saved)
        vm.cargar("f1", null)
        vm.prepararFoto("content://aquaflow.evidencias/restaurada.jpg")
        val restaurado = ChecklistViewModel(saved)
        // Android puede entregar el resultado antes del LaunchedEffect que carga la pantalla.
        restaurado.terminarFoto(true)
        restaurado.cargar("f1", null)
        assertEquals(listOf("content://aquaflow.evidencias/restaurada.jpg"), restaurado.ui.value.fotos)
        assertEquals(4, restaurado.ui.value.items.size)
    }

    @Test fun cancelarCamaraNoInventaFotosYExigeCuatroFotosObligatorias() {
        val vm = ChecklistViewModel(SavedStateHandle())
        vm.cargar("f1", null)
        assertNull(vm.guardar())
        vm.prepararFoto("content://aquaflow.evidencias/foto.jpg")
        vm.terminarFoto(false)
        assertTrue(vm.ui.value.fotos.isEmpty())
        assertNotNull(vm.ui.value.error)
        vm.ui.value.items.forEach { vm.cambiarEstado(it.id, EstadoEquipo.OBSERVADO) }
        // Con 0 fotos debe fallar al intentar guardar
        assertNull(vm.guardar())
        assertNotNull(vm.ui.value.error)
        assertTrue(vm.ui.value.error!!.contains("Falta una foto asociada"))

        // Al agregar las 4 fotos obligatorias, debe permitir guardar
        listOf("1", "2", "3", "4").forEach {
            vm.prepararFoto("content://aquaflow.evidencias/foto$it.jpg")
            vm.terminarFoto(true)
        }
        assertNotNull(vm.guardar())
        assertEquals(vm.ui.value.items.map { it.id }.toSet(), vm.guardar()!!.fotosPorEquipo.keys)

        vm.cargar("f2", null)
        assertTrue(vm.ui.value.items.all { it.estado == EstadoEquipo.PENDIENTE })
    }

    @Test fun equipoNoCumpleNoSeCompletaAunqueTengaFoto() {
        val vm = ChecklistViewModel(SavedStateHandle())
        vm.cargar("f1", null)
        vm.ui.value.items.forEach { item ->
            vm.cambiarEstado(item.id, if (item.id == "1") EstadoEquipo.NO_CUMPLE else EstadoEquipo.CUMPLE)
            vm.prepararFoto(item.id, "content://aquaflow.evidencias/${item.id}.jpg")
            vm.terminarFoto(true)
        }
        assertNull(vm.guardar())
        assertTrue(vm.ui.value.error!!.contains("fotografía no corrige"))
    }
}
