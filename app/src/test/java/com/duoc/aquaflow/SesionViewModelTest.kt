package com.duoc.aquaflow

import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.viewmodel.SesionViewModel
import org.junit.Assert.*
import org.junit.Test

class SesionViewModelTest {
    @Test fun exigeCredencialesAntesDeIngresar() {
        val vm = SesionViewModel()
        assertFalse(vm.ingresar())
        assertNotNull(vm.ingreso.value.error)
        assertNull(vm.usuario.value)
    }

    @Test fun perfilSeleccionadoNoOtorgaPermisosDeAdministrador() {
        val vm = SesionViewModel()
        vm.cambiarNombre("supervisor")
        vm.cambiarClave("1234")
        vm.seleccionarRol(Rol.JEFE)
        assertFalse(vm.ingresar())
        assertNull(vm.usuario.value)
        vm.seleccionarRol(Rol.SUPERVISOR)
        assertNull(vm.ingreso.value.error)
        assertTrue(vm.ingresar())
        assertEquals(Rol.SUPERVISOR, vm.usuario.value?.rol)
        assertEquals("", vm.ingreso.value.clave)
    }

    @Test fun administradorIngresaYSalirLimpiaElFormulario() {
        val vm = SesionViewModel()
        vm.cambiarNombre(" jefe ")
        vm.cambiarClave("1234")
        vm.seleccionarRol(Rol.JEFE)
        assertTrue(vm.ingresar())
        assertEquals(Rol.JEFE, vm.usuario.value?.rol)
        vm.cambiarClave("dato pendiente")
        vm.salir()
        assertNull(vm.usuario.value)
        assertEquals("", vm.ingreso.value.clave)
    }

    @Test fun claveIncorrectaNoIniciaSesionYEditarLimpiaElError() {
        val vm = SesionViewModel()
        vm.cambiarNombre("jefe")
        vm.cambiarClave("incorrecta")
        assertFalse(vm.ingresar())
        assertNotNull(vm.ingreso.value.error)
        assertNull(vm.usuario.value)
        vm.cambiarClave("1234")
        assertNull(vm.ingreso.value.error)
    }
}
