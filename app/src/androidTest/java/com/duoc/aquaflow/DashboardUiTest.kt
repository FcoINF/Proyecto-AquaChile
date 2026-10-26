package com.duoc.aquaflow

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.duoc.aquaflow.model.*
import com.duoc.aquaflow.ui.navigation.Tab
import com.duoc.aquaflow.ui.screens.DashboardScreen
import com.duoc.aquaflow.ui.theme.AquaFlowTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DashboardUiTest {
    @get:Rule val compose = createComposeRule()
    private val borrador = FaenaGuardada(ResumenFaena(
        PreChequeo("b1", "Centro borrador", "26/10/2026", "10:00", "Ana", "Luis"), null, null, null))
    private val autorizada = borrador.copy(
        resumen = borrador.resumen.copy(apertura = borrador.resumen.apertura.copy(id = "a1", centroCultivo = "Centro autorizado")),
        decision = RegistroDecision(Decision.AUTORIZADA, "", "jefe"))

    private fun mostrar(rol: Rol, onVer: (String) -> Unit = {}, onEditar: (String) -> Unit = {}) {
        compose.setContent {
            AquaFlowTheme {
                DashboardScreen(usuario = Usuario("Luis", "1234", rol),
                    tabs = listOf(Tab.INICIO, Tab.HISTORIAL), faenas = listOf(borrador, autorizada),
                    ultimaEncuesta = null, onNuevaFaena = {}, onEditarBorrador = onEditar,
                    onTab = {}, onSalir = {}, onVerFaena = onVer)
            }
        }
    }

    @Test fun jefeVeCuatroTarjetasSinBorradoresYAbreDetalle() {
        var id = ""
        mostrar(Rol.JEFE, onVer = { id = it })
        compose.onNodeWithText("Nueva faena").assertDoesNotExist()
        compose.onNodeWithText("Editar borrador").assertDoesNotExist()
        compose.onNodeWithText("Centro borrador").assertDoesNotExist()
        compose.onNodeWithText("Rechazadas").assertExists()
        compose.onNodeWithText("Cerradas").assertExists()
        compose.onNodeWithText("Autorizadas / en proceso").performScrollTo().performClick()
        compose.onNodeWithText("Centro autorizado").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Ver faena").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("a1", id) }
    }

    @Test fun supervisorConservaNuevaFaenaYEdicionDeSuBorrador() {
        var id = ""
        mostrar(Rol.SUPERVISOR, onEditar = { id = it })
        compose.onNodeWithText("Nueva faena").assertExists()
        compose.onNodeWithText("Centro autorizado").assertDoesNotExist()
        compose.onNodeWithText("Editar borrador").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("b1", id) }
    }
}
