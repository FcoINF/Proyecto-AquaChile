package com.duoc.aquaflow

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.runtime.mutableStateOf
import com.duoc.aquaflow.model.PreChequeo
import com.duoc.aquaflow.ui.screens.AperturaFaenaScreen
import com.duoc.aquaflow.ui.theme.AquaFlowTheme
import com.duoc.aquaflow.ui.theme.AquaVerde
import com.duoc.aquaflow.viewmodel.FaenaUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AperturaFaenaUiTest {
    @get:Rule
    val compose = createComposeRule()

    private fun mostrar(apertura: PreChequeo = aperturaDemo(), onCambiar: (PreChequeo) -> Unit = {}) {
        val aperturaActual = mutableStateOf(apertura)
        compose.setContent {
            AquaFlowTheme {
                AperturaFaenaScreen(
                    ui = FaenaUiState(apertura = aperturaActual.value),
                    onCambiar = {
                        aperturaActual.value = it
                        onCambiar(it)
                    },
                    onGuardar = {},
                    onRecuperar = {},
                    onContinuar = {},
                    onSalir = {}
                )
            }
        }
    }

    @Test
    fun condicionesClimaticasMuestranValoresReferencialesYEstadoApta() {
        mostrar()

        compose.onNodeWithText("Condiciones Climáticas").fetchSemanticsNode()
        compose.onNodeWithText("12").fetchSemanticsNode()
        compose.onNodeWithText("NUDOS SO").fetchSemanticsNode()
        compose.onNodeWithTag("weather-operational-limit").fetchSemanticsNode().config
            .getOrElse(SemanticsProperties.Text) { emptyList() }
            .single()
            .let { text ->
                assertEquals("LÍMITE OPERATIVO < 20 kts", text.text)
                val segmentosNegrita = text.spanStyles
                        .filter { it.item.fontWeight == androidx.compose.ui.text.font.FontWeight.Bold }
                        .map { text.text.substring(it.start, it.end) }
                        .toSet()
                assertEquals(
                        setOf("LÍMITE OPERATIVO", "< 20 kts"),
                        segmentosNegrita
                )
            }
        compose.onNodeWithText("Favorable").fetchSemanticsNode()
        compose.onNodeWithText("Reposo marea alta (09:45)").fetchSemanticsNode()
        compose.onNodeWithText("APTA").fetchSemanticsNode()
        compose.onNodeWithText(
            "Valores referenciales de presentación; verificar en terreno antes de operar."
        ).fetchSemanticsNode()
        compose.onAllNodesWithText("DEMO").assertCountEquals(0)
        compose.onAllNodesWithText("Datos simulados", substring = true).assertCountEquals(0)
        compose.onAllNodesWithText("Valores de demostración").assertCountEquals(0)

        val pixelApta = compose.onNodeWithTag("clima-apta")
            .captureToImage().toPixelMap().let { it[1, it.height / 2] }
        assertEquals(AquaVerde, pixelApta)
        compose.onAllNodesWithText("SIN DATOS").assertCountEquals(0)
    }

    @Test
    fun confirmacionMeteorologicaExigeVerificacionRealSeparadaDeValoresReferenciales() {
        var actualizada: PreChequeo? = null
        mostrar(onCambiar = { actualizada = it })

        compose.onAllNodesWithText("Mar y meteorología aptas", substring = true)
            .assertCountEquals(0)
        compose.onNodeWithTag("weather-manual-confirmation").assertIsOff()
        compose.onNodeWithText("Confirmo la verificación meteorológica real en terreno")
            .performScrollTo()
            .performClick()

        assertEquals(true, actualizada?.condicionesListas)
        val confirmacion = compose.onNodeWithTag("weather-manual-confirmation")
            .captureToImage().toPixelMap()
        val contieneVerde = (0 until confirmacion.height).any { y ->
            (0 until confirmacion.width).any { x ->
                val pixel = confirmacion[x, y]
                pixel.green > 0.35f && pixel.green > pixel.red * 1.2f && pixel.green > pixel.blue
            }
        }
        assertTrue("La confirmación seleccionada debe mostrar color verde", contieneVerde)
    }

    @Test
    fun condicionesInicialesUsanOpcionesSiNoEnLugarDeCheckboxes() {
        var actualizada: PreChequeo? = null
        mostrar(onCambiar = { actualizada = it })

        compose.onAllNodesWithText("APTO", substring = true).assertCountEquals(0)
        val botonSi = compose.onNodeWithTag("embarcacion-si").performScrollTo()
        val botonNo = compose.onNodeWithTag("embarcacion-no")
        val boundsSi = botonSi.fetchSemanticsNode().boundsInRoot
        val boundsNo = botonNo.fetchSemanticsNode().boundsInRoot
        assertEquals(boundsNo.width, boundsSi.width, 1.5f)
        assertEquals(boundsNo.height, boundsSi.height, 1.5f)
        // Sin respuesta, ninguna de las dos opciones debe aparecer seleccionada.
        botonNo.assertIsNotSelected()
        compose.onNodeWithTag("embarcacion-si").assertIsNotSelected()
        compose.onNodeWithTag("comunicaciones-no").performScrollTo().assertIsNotSelected()
        compose.onNodeWithTag("comunicaciones-si").assertIsNotSelected()

        compose.onNodeWithTag("embarcacion-si").performScrollTo().performClick()
        assertEquals(true, actualizada?.embarcacionLista)
        compose.onNodeWithTag("comunicaciones-si").performScrollTo().performClick()
        assertEquals(true, actualizada?.comunicacionesOperativas)
        compose.onNodeWithTag("comunicaciones-no").performScrollTo().performClick()
        assertEquals(false, actualizada?.comunicacionesOperativas)
    }

    @Test
    fun datosOperativosConservanCentroBuzoYFechaHoraSinMostrarSupervisor() {
        mostrar()

        compose.onNodeWithText("Datos Operativos de Faena").fetchSemanticsNode()
        compose.onNodeWithText("Centro de cultivo").performScrollTo().fetchSemanticsNode()
        compose.onNodeWithText("Fecha de faena").performScrollTo().fetchSemanticsNode()
        compose.onNodeWithText("25/10/2026").performScrollTo().fetchSemanticsNode()
        compose.onNodeWithText("Hora de faena").performScrollTo().fetchSemanticsNode()
        compose.onNodeWithText("09:30").performScrollTo().fetchSemanticsNode()
        compose.onNodeWithText("Buzo Técnico Asignado").performScrollTo().fetchSemanticsNode()
        compose.onNodeWithText("Buzo asignado").performScrollTo().fetchSemanticsNode()
        compose.onAllNodesWithText("Supervisor de la sesión", substring = true)
            .assertCountEquals(0)
    }
}

private fun aperturaDemo() = PreChequeo(
    id = "demo",
    centroCultivo = "Centro Punta de Tralca",
    fecha = "25/10/2026",
    hora = "09:30",
    buzoAsignado = "Juan Pérez",
    supervisorACargo = "Supervisor Demo"
)
