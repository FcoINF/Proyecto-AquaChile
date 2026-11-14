package com.duoc.aquaflow

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.duoc.aquaflow.navigation.AquaFlowNavHost
import com.duoc.aquaflow.ui.theme.AquaFlowTheme
import org.junit.Rule
import org.junit.Test

class NavegacionFaenaUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun supervisorIngresaYAbreElFlujoDeCincoPasos() {
        compose.setContent { AquaFlowTheme { AquaFlowNavHost() } }

        compose.onNodeWithText("Supervisor de Buceo").performScrollTo().performClick()
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("supervisor")
        compose.onAllNodes(hasSetTextAction())[1].performTextInput("1234")
        compose.onNodeWithText("INGRESAR").performScrollTo().performClick()

        compose.waitUntil(20_000) {
            compose.onAllNodesWithText("Nueva faena y checklist AST").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Nueva faena y checklist AST").performScrollTo().performClick()
        compose.onNodeWithText("Paso 1 de 5").assertExists()
        compose.onNodeWithText("Condiciones en Pontón").assertExists()
    }
}
