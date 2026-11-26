package com.duoc.aquaflow

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextLayoutResult
import com.duoc.aquaflow.ui.screens.LoginScreen
import com.duoc.aquaflow.ui.theme.AquaFlowTheme
import com.duoc.aquaflow.viewmodel.SesionViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

class LoginUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun completaYCorrigePinYEnviaDesdeElTeclado() {
        val sesion = SesionViewModel()
        var ingresos = 0
        compose.setContent {
            AquaFlowTheme {
                LoginScreen(sesion) { ingresos++ }
            }
        }

        val usuario = compose.onNode(hasContentDescription("USUARIO DEL OPERADOR") and hasSetTextAction())
        usuario.performTextReplacement("")
        compose.onNodeWithText("Rol", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Contraseña", useUnmergedTree = true).assertExists()
        usuario.performTextReplacement("supervisor")
        val pin = compose.onNode(hasContentDescription("PIN DE ACCESO") and hasSetTextAction())
        pin.performScrollTo().performTextInput("12")
        pin.performTextInput("53")
        pin.performTextReplacement("123")
        pin.performTextInput("4")
        compose.onNodeWithContentDescription("Mostrar contraseña").performClick()
        val visible = mutableListOf<TextLayoutResult>()
        pin.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(visible) }
        assertEquals("1234", visible.single().layoutInput.text.text)
        compose.onNodeWithContentDescription("Ocultar contraseña").performClick()
        val oculta = mutableListOf<TextLayoutResult>()
        pin.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(oculta) }
        assertNotEquals("1234", oculta.single().layoutInput.text.text)
        pin.performImeAction()

        compose.waitUntil { sesion.usuario.value?.nombre == "supervisor" }
        compose.runOnIdle { assertEquals(1, ingresos) }
    }
}
