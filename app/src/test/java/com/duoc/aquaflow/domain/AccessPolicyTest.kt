package com.duoc.aquaflow.domain

import com.duoc.aquaflow.TestData
import com.duoc.aquaflow.domain.rules.AccessPolicy
import com.duoc.aquaflow.domain.rules.Destino
import com.duoc.aquaflow.model.EstadoFaena
import com.duoc.aquaflow.model.Rol
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessPolicyTest {
    private val supervisor = TestData.usuario(id = 1, rol = Rol.SUPERVISOR)
    private val otroSupervisor = TestData.usuario(id = 2, rol = Rol.SUPERVISOR)
    private val admin = TestData.usuario(id = 3, rol = Rol.ADMINISTRADOR)

    @Test
    fun `solo el supervisor responsable edita el prechequeo`() {
        val faena = TestData.faena(supervisorId = 1, estado = EstadoFaena.PRECHEQUEO)
        assertTrue(AccessPolicy.puedeEditarPrechequeo(supervisor, faena))
        assertFalse(AccessPolicy.puedeEditarPrechequeo(otroSupervisor, faena))
        assertFalse(AccessPolicy.puedeEditarPrechequeo(admin, faena))
    }

    @Test
    fun `no se edita una faena que ya salio de prechequeo`() {
        EstadoFaena.values().filter { it != EstadoFaena.PRECHEQUEO }.forEach { estado ->
            assertFalse(AccessPolicy.puedeEditarPrechequeo(supervisor, TestData.faena(estado = estado)))
        }
    }

    @Test
    fun `solo el administrador decide y solo con faena pendiente`() {
        val pendiente = TestData.faena(estado = EstadoFaena.PENDIENTE_AUTORIZACION)
        assertTrue(AccessPolicy.puedeDecidir(admin, pendiente))
        assertFalse(AccessPolicy.puedeDecidir(supervisor, pendiente))
        assertFalse(AccessPolicy.puedeDecidir(admin, TestData.faena(estado = EstadoFaena.PRECHEQUEO)))
        assertFalse(AccessPolicy.puedeDecidir(admin, TestData.faena(estado = EstadoFaena.AUTORIZADA)))
    }

    @Test
    fun `el registro posterior es del supervisor responsable y solo si esta autorizada`() {
        val autorizada = TestData.faena(supervisorId = 1, estado = EstadoFaena.AUTORIZADA)
        assertTrue(AccessPolicy.puedeRegistrarPosterior(supervisor, autorizada))
        assertFalse(AccessPolicy.puedeRegistrarPosterior(otroSupervisor, autorizada))
        assertFalse(AccessPolicy.puedeRegistrarPosterior(admin, autorizada))
        assertFalse(AccessPolicy.puedeRegistrarPosterior(supervisor, TestData.faena(supervisorId = 1, estado = EstadoFaena.PENDIENTE_AUTORIZACION)))
    }

    @Test
    fun `un supervisor no ve faenas ajenas pero el administrador ve todas`() {
        val faena = TestData.faena(supervisorId = 1)
        assertTrue(AccessPolicy.puedeVer(supervisor, faena))
        assertFalse(AccessPolicy.puedeVer(otroSupervisor, faena))
        assertTrue(AccessPolicy.puedeVer(admin, faena))
    }

    @Test
    fun `un usuario inactivo no puede hacer nada`() {
        val inactivo = TestData.usuario(id = 1, rol = Rol.SUPERVISOR, activo = false)
        val faena = TestData.faena(supervisorId = 1)
        assertFalse(AccessPolicy.puedeVer(inactivo, faena))
        assertFalse(AccessPolicy.puedeEditarPrechequeo(inactivo, faena))
        assertFalse(AccessPolicy.puedeCrearFaena(inactivo))
    }

    @Test
    fun `las pantallas se restringen por rol`() {
        assertFalse(AccessPolicy.puedeAccederA(Rol.SUPERVISOR, Destino.USUARIOS))
        assertTrue(AccessPolicy.puedeAccederA(Rol.ADMINISTRADOR, Destino.USUARIOS))
        assertTrue(AccessPolicy.puedeAccederA(Rol.SUPERVISOR, Destino.APERTURA))
        assertFalse(AccessPolicy.puedeAccederA(Rol.ADMINISTRADOR, Destino.APERTURA))
        assertTrue(AccessPolicy.puedeAccederA(Rol.ADMINISTRADOR, Destino.HISTORIAL))
    }

    @Test
    fun `solo el administrador gestiona usuarios y solo el supervisor crea faenas`() {
        assertTrue(AccessPolicy.puedeGestionarUsuarios(admin))
        assertFalse(AccessPolicy.puedeGestionarUsuarios(supervisor))
        assertTrue(AccessPolicy.puedeCrearFaena(supervisor))
        assertFalse(AccessPolicy.puedeCrearFaena(admin))
    }
}
