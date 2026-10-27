package com.duoc.aquaflow

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsProperties
import com.duoc.aquaflow.model.PREGUNTAS_SALUD
import com.duoc.aquaflow.ui.screens.EncuestaSaludScreen
import com.duoc.aquaflow.ui.theme.AquaFlowTheme
import com.duoc.aquaflow.ui.navigation.Tab
import com.duoc.aquaflow.viewmodel.EncuestaSaludViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EncuestaSaludUiTest {
    @get:Rule
    val compose = createComposeRule()

    private fun mostrar(vm: EncuestaSaludViewModel) {
        compose.setContent {
            AquaFlowTheme {
                EncuestaSaludScreen(
                    vm = vm,
                    buzo = "Buzo de prueba",
                    tabs = listOf(Tab.INICIO, Tab.ENCUESTA, Tab.HISTORIAL),
                    onGuardar = {},
                    onGuardarBorrador = {},
                    onVolver = {},
                    onTab = {}
                )
            }
        }
    }

    @Test
    fun steppersAjustanLosValoresMedidos() {
        val vm = EncuestaSaludViewModel().apply {
            cambiarSistolica("120")
            cambiarDiastolica("80")
            cambiarSaturacion("98")
        }
        mostrar(vm)

        compose.onNodeWithContentDescription("Aumentar Sistólica").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Disminuir Saturación O₂").performScrollTo().performClick()

        assertEquals("121", vm.ui.value.sistolica)
        assertEquals("97", vm.ui.value.saturacion)
    }

    @Test
    fun respuestasSiYNoActualizanLaAlertaSeleccionada() {
        val vm = EncuestaSaludViewModel()
        mostrar(vm)

        assertEquals(false, compose.onNodeWithTag("pregunta-0-no").performScrollTo().fetchSemanticsNode().config[SemanticsProperties.Selected])
        compose.onNodeWithTag("pregunta-0-si").performScrollTo().performClick()
        assertEquals(setOf(PREGUNTAS_SALUD[0]), vm.ui.value.alertas)
        compose.onNodeWithTag("pregunta-0-si").performScrollTo().assertIsSelected()

        compose.onNodeWithTag("pregunta-0-no").performScrollTo().performClick()
        compose.onNodeWithTag("pregunta-0-no").performScrollTo().assertIsSelected()
        assertEquals(emptySet<String>(), vm.ui.value.alertas)
    }

    @Test
    fun ningunaRespuestaEstaSeleccionadaAlAbrirLaEncuesta() {
        val vm = EncuestaSaludViewModel()
        mostrar(vm)

        PREGUNTAS_SALUD.indices.forEach { indice ->
            assertEquals(false, compose.onNodeWithTag("pregunta-$indice-si").performScrollTo().fetchSemanticsNode().config[SemanticsProperties.Selected])
            assertEquals(false, compose.onNodeWithTag("pregunta-$indice-no").performScrollTo().fetchSemanticsNode().config[SemanticsProperties.Selected])
        }
    }

    @Test
    fun respuestasMuestranLosIconosCorrespondientes() {
        val vm = EncuestaSaludViewModel()
        mostrar(vm)

        assertEquals(false, compose.onNodeWithTag("pregunta-0-no").performScrollTo().fetchSemanticsNode().config[SemanticsProperties.Selected])
        assertEquals(false, compose.onNodeWithTag("pregunta-0-si").performScrollTo().fetchSemanticsNode().config[SemanticsProperties.Selected])
        assertEquals(3, compose.onAllNodesWithContentDescription("Check positivo de Si").fetchSemanticsNodes().size)
        assertEquals(3, compose.onAllNodesWithContentDescription("X de No").fetchSemanticsNodes().size)

        compose.onNodeWithTag("pregunta-0-si").performScrollTo().performClick()

        compose.onNodeWithTag("pregunta-0-si").performScrollTo().assertIsSelected()
        assertEquals(false, compose.onNodeWithTag("pregunta-0-no").performScrollTo().fetchSemanticsNode().config[SemanticsProperties.Selected])
    }

    @Test
    fun siSeleccionadoEsRojoYNoSeleccionadoEsVerde() {
        val vm = EncuestaSaludViewModel()
        mostrar(vm)

        compose.onNodeWithTag("pregunta-0-si").performScrollTo().performClick()
        val colorSi = compose.onNodeWithTag("pregunta-0-si")
            .captureToImage().toPixelMap().let { it[it.width / 4, it.height / 2] }
        assertTrue("Sí debe mostrar un color rojo", colorSi.red > colorSi.green && colorSi.red > colorSi.blue)

        compose.onNodeWithTag("pregunta-0-no").performScrollTo().performClick()
        val colorNo = compose.onNodeWithTag("pregunta-0-no")
            .captureToImage().toPixelMap().let { it[it.width / 4, it.height / 2] }
        assertTrue("No debe mostrar un color verde", colorNo.green > colorNo.red && colorNo.green > colorNo.blue)
    }

    @Test
    fun steppersRespetanLimitesYNoAjustanValoresVacios() {
        val vm = EncuestaSaludViewModel().apply {
            cambiarSistolica("250")
            cambiarDiastolica("80")
        }
        mostrar(vm)

        compose.onNodeWithContentDescription("Aumentar Sistólica").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithContentDescription("Disminuir Sistólica").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Aumentar Saturación O₂").performScrollTo().assertIsNotEnabled()

        assertEquals("249", vm.ui.value.sistolica)
        assertEquals("", vm.ui.value.saturacion)
    }
}
