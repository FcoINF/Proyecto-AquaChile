package com.duoc.aquaflow.viewmodel

import com.duoc.aquaflow.FakeEvidenciaArchivos
import com.duoc.aquaflow.FakeFaenaRepository
import com.duoc.aquaflow.MainDispatcherRule
import com.duoc.aquaflow.TestData
import com.duoc.aquaflow.domain.AppError
import com.duoc.aquaflow.domain.AppResult
import com.duoc.aquaflow.domain.config.ReglasConfig
import com.duoc.aquaflow.model.EstadoEquipo
import com.duoc.aquaflow.model.EstadoFaena
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.model.TipoEvidencia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class Dpr24ViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val supervisor = TestData.usuario(id = 1, rol = Rol.SUPERVISOR)
    private val admin = TestData.usuario(id = 3, rol = Rol.ADMINISTRADOR)
    private val archivos = FakeEvidenciaArchivos()

    private fun crearVm(usuario: com.duoc.aquaflow.model.Usuario, repo: FakeFaenaRepository) =
        Dpr24ViewModel(usuario, 10, repo, archivos, ReglasConfig())

    @Test
    fun `carga las respuestas guardadas y habilita la edicion al supervisor responsable`() {
        val repo = FakeFaenaRepository(TestData.detalleCompleto())
        val vm = crearVm(supervisor, repo)

        assertTrue(vm.state.value.editable)
        assertEquals(4, vm.state.value.items.size)
        assertFalse(vm.state.value.sinGuardar)
    }

    @Test
    fun `cambiar un estado marca cambios sin guardar y guardar los envia`() {
        val repo = FakeFaenaRepository(TestData.detalleVacio())
        val vm = crearVm(supervisor, repo)

        vm.onEstado("EQ_COMPRESOR", EstadoEquipo.OBSERVADO)
        vm.onNota("EQ_COMPRESOR", "Purga lenta")
        assertTrue(vm.state.value.sinGuardar)

        vm.guardar()

        val enviado = repo.ultimoChecklist!!.single()
        assertEquals("EQ_COMPRESOR", enviado.itemId)
        assertEquals(EstadoEquipo.OBSERVADO, enviado.estado)
        assertEquals("Purga lenta", enviado.nota)
        assertFalse(vm.state.value.sinGuardar)
    }

    @Test
    fun `volver al valor guardado deja de contar como cambio`() {
        val repo = FakeFaenaRepository(TestData.detalleCompleto())
        val vm = crearVm(supervisor, repo)

        vm.onEstado("EQ_COMPRESOR", EstadoEquipo.NO_CUMPLE)
        assertTrue(vm.state.value.sinGuardar)
        vm.onEstado("EQ_COMPRESOR", EstadoEquipo.CUMPLE)
        assertFalse(vm.state.value.sinGuardar)
    }

    @Test
    fun `un error al guardar mantiene los cambios y muestra el mensaje`() {
        val repo = FakeFaenaRepository(TestData.detalleVacio())
        repo.resultadoChecklist = AppResult.Fail(AppError.Forbidden)
        val vm = crearVm(supervisor, repo)

        vm.onEstado("EQ_TRAJE", EstadoEquipo.CUMPLE)
        vm.guardar()

        assertTrue(vm.state.value.sinGuardar)
        assertEquals(AppError.Forbidden.mensaje, vm.state.value.mensaje)
    }

    @Test
    fun `el administrador solo puede mirar`() {
        val repo = FakeFaenaRepository(TestData.detalleCompleto())
        val vm = crearVm(admin, repo)

        assertFalse(vm.state.value.editable)
        vm.onEstado("EQ_COMPRESOR", EstadoEquipo.NO_CUMPLE)
        assertFalse(vm.state.value.sinGuardar)
        assertEquals(EstadoEquipo.CUMPLE, vm.state.value.items["EQ_COMPRESOR"]!!.estado)
    }

    @Test
    fun `una faena fuera de prechequeo no es editable`() {
        val detalle = TestData.detalleCompleto(TestData.faena(estado = EstadoFaena.PENDIENTE_AUTORIZACION))
        val vm = crearVm(supervisor, FakeFaenaRepository(detalle))
        assertFalse(vm.state.value.editable)
    }

    @Test
    fun `si la evidencia no se puede registrar se borra el archivo huerfano`() {
        val repo = FakeFaenaRepository(TestData.detalleCompleto())
        repo.resultadoEvidencia = AppResult.Fail(AppError.Forbidden)
        val vm = crearVm(supervisor, repo)

        vm.agregarEvidencia(TipoEvidencia.FILTROS, null, "10/foto.jpg")

        assertEquals(listOf("10/foto.jpg"), archivos.eliminados)
    }

    @Test
    fun `una evidencia registrada correctamente conserva el archivo`() {
        val repo = FakeFaenaRepository(TestData.detalleCompleto())
        val vm = crearVm(supervisor, repo)

        vm.agregarEvidencia(TipoEvidencia.FILTROS, null, "10/foto.jpg")

        assertTrue(archivos.eliminados.isEmpty())
    }
}
