package com.duoc.aquaflow

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import com.duoc.aquaflow.navigation.AquaFlowNavHost
import com.duoc.aquaflow.ui.theme.AquaFlowTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NavegacionFaenaUiTest {
    @get:Rule val compose = createComposeRule()

    private fun click(texto: String, scroll: Boolean = false) {
        val nodo = compose.onNodeWithText(texto)
        if (scroll) nodo.performScrollTo()
        nodo.performClick()
    }

    private fun abrirNuevaHastaSalud() {
        click("Nueva faena", true)
        click("Centro de cultivo", true)
        click("Centro Punta de Tralca")
        click("Buzo asignado", true)
        click("Juan Pérez")
        listOf("Mar y meteorología aptas", "Embarcación o plataforma de apoyo lista", "Comunicaciones y respuesta de emergencia operativas").forEach { click(it, true) }
        click("Continuar al checklist", true)
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Inspección de equipos").fetchSemanticsNodes().isNotEmpty() }
        for (id in 1..4) {
            compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag("equipo-$id"))
            compose.onNode(hasText("Cumple") and hasAnyAncestor(hasTestTag("equipo-$id"))).performClick()
        }
        click("Guardar y continuar")
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Control preventivo").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun nuevaFaenaNoHeredaSaludDeLaAnterior() {
        compose.setContent { AquaFlowTheme { AquaFlowNavHost() } }
        compose.onNodeWithText("SUPERVISOR", substring = false).performScrollTo().performClick()
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("supervisor")
        compose.onAllNodes(hasSetTextAction())[1].performTextInput("1234")
        click("INICIAR SESIÓN", true)
        abrirNuevaHastaSalud()
        val valores = listOf("120", "80", "98")
        valores.forEachIndexed { indice, valor ->
            compose.onAllNodes(hasSetTextAction())[indice].performScrollTo().performTextInput(valor)
        }
        click("Guardar", true)
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Formulario AST").fetchSemanticsNodes().isNotEmpty() }
        click("Volver")
        click("Inicio")
        abrirNuevaHastaSalud()
        compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().forEach {
            assertTrue(it.config[SemanticsProperties.EditableText].text.isEmpty())
        }
    }
}

