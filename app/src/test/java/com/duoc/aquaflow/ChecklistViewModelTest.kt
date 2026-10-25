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
        vm.prepararFoto("content://aquaflow.evidencias/foto.jpg")
        val restaurado = ChecklistViewModel(saved)
        restaurado.cargar("f1", null)
        assertEquals(vm.ui.value, restaurado.ui.value)
        restaurado.terminarFoto(true)
        assertEquals(listOf("content://aquaflow.evidencias/foto.jpg"), restaurado.guardar()!!.fotosEvidencia)
        assertNull(restaurado.ui.value.pendiente)
    }

    @Test fun cancelarCamaraNoInventaFotosYExigeRevisarTodosLosEquipos() {
        val vm = ChecklistViewModel(SavedStateHandle())
        vm.cargar("f1", null)
        assertNull(vm.guardar())
        vm.prepararFoto("content://aquaflow.evidencias/foto.jpg")
        vm.terminarFoto(false)
        assertTrue(vm.ui.value.fotos.isEmpty())
        assertNotNull(vm.ui.value.error)
        vm.ui.value.items.forEach { vm.cambiarEstado(it.id, EstadoEquipo.OBSERVADO) }
        assertNotNull(vm.guardar())
        vm.cargar("f2", null)
        assertTrue(vm.ui.value.items.all { it.estado == EstadoEquipo.PENDIENTE })
    }
}
