package com.duoc.aquaflow.domain

import com.duoc.aquaflow.domain.rules.FaenaStateMachine
import com.duoc.aquaflow.model.EstadoFaena
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FaenaStateMachineTest {

    @Test
    fun `el flujo normal es posible paso a paso`() {
        assertTrue(FaenaStateMachine.puedePasar(EstadoFaena.PRECHEQUEO, EstadoFaena.PENDIENTE_AUTORIZACION))
        assertTrue(FaenaStateMachine.puedePasar(EstadoFaena.PENDIENTE_AUTORIZACION, EstadoFaena.AUTORIZADA))
        assertTrue(FaenaStateMachine.puedePasar(EstadoFaena.AUTORIZADA, EstadoFaena.CERRADA))
    }

    @Test
    fun `se puede rechazar o cancelar en el punto correcto`() {
        assertTrue(FaenaStateMachine.puedePasar(EstadoFaena.PENDIENTE_AUTORIZACION, EstadoFaena.RECHAZADA))
        assertTrue(FaenaStateMachine.puedePasar(EstadoFaena.PRECHEQUEO, EstadoFaena.CANCELADA))
    }

    @Test
    fun `no se puede saltar la autorizacion`() {
        assertFalse(FaenaStateMachine.puedePasar(EstadoFaena.PRECHEQUEO, EstadoFaena.AUTORIZADA))
        assertFalse(FaenaStateMachine.puedePasar(EstadoFaena.PRECHEQUEO, EstadoFaena.CERRADA))
        assertFalse(FaenaStateMachine.puedePasar(EstadoFaena.PENDIENTE_AUTORIZACION, EstadoFaena.CERRADA))
    }

    @Test
    fun `no se puede retroceder`() {
        assertFalse(FaenaStateMachine.puedePasar(EstadoFaena.AUTORIZADA, EstadoFaena.PRECHEQUEO))
        assertFalse(FaenaStateMachine.puedePasar(EstadoFaena.PENDIENTE_AUTORIZACION, EstadoFaena.PRECHEQUEO))
        assertFalse(FaenaStateMachine.puedePasar(EstadoFaena.AUTORIZADA, EstadoFaena.RECHAZADA))
    }

    @Test
    fun `los estados terminales no tienen salida`() {
        listOf(EstadoFaena.RECHAZADA, EstadoFaena.CANCELADA, EstadoFaena.CERRADA).forEach { terminal ->
            assertTrue(FaenaStateMachine.destinosDe(terminal).isEmpty())
            assertTrue(terminal.esTerminal)
        }
    }

    @Test
    fun `validar devuelve error solo en transiciones invalidas`() {
        assertNull(FaenaStateMachine.validar(EstadoFaena.PRECHEQUEO, EstadoFaena.PENDIENTE_AUTORIZACION))
        val error = FaenaStateMachine.validar(EstadoFaena.CERRADA, EstadoFaena.AUTORIZADA)
        assertNotNull(error)
        assertEquals(true, error!!.mensaje.contains("Cerrada"))
    }
}
