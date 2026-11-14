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

    private fun mostrar(rol: Rol, onVer: (String) -> Unit = {}, onBorradores: () -> Unit = {}) {
        compose.setContent {
            AquaFlowTheme {
                DashboardScreen(usuario = Usuario("Luis", "1234", rol),
                    tabs = listOf(Tab.INICIO, Tab.HISTORIAL), faenas = listOf(borrador, autorizada),
                    onNuevaFaena = {}, onAbrirBorradores = onBorradores,
                    onTab = {}, onSalir = {}, onVerFaena = onVer)
            }
        }
    }

    @Test fun jefeVeCuatroTarjetasSinBorradoresYAbreDetalle() {
        var id = ""
        mostrar(Rol.JEFE, onVer = { id = it })
        compose.onNodeWithText("Nueva faena y checklist AST").assertDoesNotExist()
        compose.onNodeWithText("Centro borrador").assertDoesNotExist()
        compose.onNodeWithText("Rechazadas").assertExists()
        compose.onNodeWithText("Cerradas").assertExists()
        compose.onNodeWithText("Autorizadas").performScrollTo().performClick()
        compose.onNodeWithText("Centro autorizado").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Revisar Detalle de Operación").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("a1", id) }
    }

    @Test fun supervisorConservaNuevaFaenaYEdicionDeSuBorrador() {
        var id = ""
        mostrar(Rol.SUPERVISOR, onBorradores = { id = "b1" })
        compose.onNodeWithText("Nueva faena y checklist AST").assertExists()
        compose.onNodeWithText("Borradores por completar").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("b1", id) }
    }
}
