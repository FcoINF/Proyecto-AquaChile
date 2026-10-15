package com.duoc.aquaflow.viewmodel

import com.duoc.aquaflow.FakeAuthRepository
import com.duoc.aquaflow.MainDispatcherRule
import com.duoc.aquaflow.domain.AppError
import com.duoc.aquaflow.domain.AppResult
import com.duoc.aquaflow.model.Rol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LoginViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private val vm = LoginViewModel(auth)

    @Test
    fun `el PIN solo acepta digitos y maximo cuatro`() {
        vm.onPinChange("12a3-45")
        assertEquals("1234", vm.state.value.pin)
    }

    @Test
    fun `el RUT se limita en largo y caracteres`() {
        vm.onRutChange("15.892.403-K!!!!!!")
        assertTrue(vm.state.value.rut.length <= 12)
        assertFalse(vm.state.value.rut.contains('!'))
    }

    @Test
    fun `credenciales incorrectas muestran el error y limpian el PIN`() {
        auth.resultadoLogin = AppResult.Fail(AppError.InvalidCredentials)
        vm.onRutChange("15892403-K")
        vm.onPinChange("0000")
        vm.ingresar()

        val estado = vm.state.value
        assertEquals(AppError.InvalidCredentials.mensaje, estado.error)
        assertEquals("", estado.pin)
        assertFalse(estado.cargando)
    }

    @Test
    fun `los errores de validacion se asignan al campo correspondiente`() {
        auth.resultadoLogin = AppResult.Fail(AppError.Validation(mapOf("rut" to "RUT inválido.")))
        vm.onRutChange("123")
        vm.onPinChange("1234")
        vm.ingresar()

        assertEquals("RUT inválido.", vm.state.value.errorRut)
        assertNull(vm.state.value.error)
    }

    @Test
    fun `un acceso correcto reinicia el formulario y usa el rol elegido`() {
        vm.onRolChange(Rol.ADMINISTRADOR)
        vm.onRutChange("12433910-3")
        vm.onPinChange("2468")
        vm.ingresar()

        assertEquals(Triple("12433910-3", "2468", Rol.ADMINISTRADOR), auth.llamadasLogin.single())
        assertEquals("", vm.state.value.rut)
        assertEquals("", vm.state.value.pin)
        assertEquals(Rol.ADMINISTRADOR, vm.state.value.rol)
    }
}
