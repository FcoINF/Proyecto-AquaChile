package com.duoc.aquaflow

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.duoc.aquaflow.model.RegistroCierre
import com.duoc.aquaflow.ui.components.CierreFaenaFormulario
import com.duoc.aquaflow.ui.theme.AquaFlowTheme
import androidx.compose.foundation.layout.Column
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CierreFormularioUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun requiereDatosYConfirmacionAntesDeEnviarCierre() {
        var enviado: RegistroCierre? = null
        compose.setContent { AquaFlowTheme { Column { CierreFaenaFormulario("f1") { enviado = it } } } }
        compose.onNodeWithText("Cerrar faena").performClick()
        compose.onNodeWithText("Ingresa la duración", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Tiempo de inmersión (minutos)").performTextInput("25")
        compose.onNodeWithText("Observaciones de salida").performTextInput("Sin novedades")
        compose.onNode(isToggleable()).performClick()
        compose.onNodeWithText("Cerrar faena").performClick()
        compose.runOnIdle { assertNull(enviado) }
        compose.onNodeWithText("Confirmar cierre", substring = false).performClick()
        compose.runOnIdle { assertEquals(25, enviado!!.duracionMinutos); assertTrue(enviado!!.revisionSalida) }
    }
}
