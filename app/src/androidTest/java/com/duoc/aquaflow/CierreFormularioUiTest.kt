package com.duoc.aquaflow

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.duoc.aquaflow.model.RegistroCierre
import com.duoc.aquaflow.ui.components.CierreFaenaFormulario
import com.duoc.aquaflow.ui.theme.AquaFlowTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Modifier
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CierreFormularioUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun requiereDatosYConfirmacionAntesDeEnviarCierre() {
        var enviado: RegistroCierre? = null
        compose.setContent { AquaFlowTheme { Column(Modifier.verticalScroll(rememberScrollState())) { CierreFaenaFormulario("f1") { enviado = it } } } }
        compose.onNodeWithText("Cerrar faena").performScrollTo().performClick()
        compose.onNodeWithText("Ingresa la duración", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Tiempo de inmersión (minutos)").performScrollTo().performTextInput("25")
        compose.onNodeWithText("Observaciones de salida").performScrollTo().performTextInput("Sin novedades")
        compose.onAllNodes(isToggleable())[0].performScrollTo().performClick()
        compose.onNodeWithText("Presión sistólica de salida").performScrollTo().performTextInput("125")
        compose.onNodeWithText("Presión diastólica de salida").performScrollTo().performTextInput("82")
        compose.onNodeWithText("Saturación de salida (%)").performScrollTo().performTextInput("97")
        compose.onNodeWithText("Cerrar faena").performScrollTo().performClick()
        compose.runOnIdle { assertNull(enviado) }
        compose.onNodeWithText("Confirmar cierre", substring = false).performClick()
        compose.runOnIdle { assertEquals(25, enviado!!.duracionMinutos); assertTrue(enviado!!.revisionSalida) }
    }
}
