package com.duoc.aquaflow

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.runtime.mutableStateOf
import com.duoc.aquaflow.model.PreChequeo
import com.duoc.aquaflow.model.CENTROS_MARITIMOS
import com.duoc.aquaflow.model.CondicionesMaritimas
import com.duoc.aquaflow.viewmodel.MeteorologiaUi
import com.duoc.aquaflow.data.repository.ResultadoMeteorologia
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

    private fun mostrar(
        apertura: PreChequeo = aperturaDemo(),
        onCambiar: (PreChequeo) -> Unit = {},
        meteorologia: MeteorologiaUi = MeteorologiaUi()
    ) {
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
                    onSalir = {},
                    meteorologia = meteorologia
                )
            }
        }
    }

    @Test
    fun condicionesMaritimasMuestranOleajeUbicacionYHoraSinAutorizarLaFaena() {
        val centro = CENTROS_MARITIMOS.first()
        val datos = CondicionesMaritimas(-42.0, -73.0, 1_790_000_000_000, 1_789_999_200_000, 1.5, 7.0, 240.0, null)
        mostrar(aperturaDemo().copy(centroId = centro.id), meteorologia =
            MeteorologiaUi(centro, resultado = ResultadoMeteorologia.Disponible(datos)))

        compose.onNodeWithText("Meteorología marítima").fetchSemanticsNode()
        compose.onNodeWithText("Golfo de Ancud", substring = true).fetchSemanticsNode()
        compose.onNodeWithText("Oleaje: 1,5 m", substring = true).fetchSemanticsNode()
        compose.onNodeWithText("Ver detalles del modelo").performScrollTo().performClick()
        compose.onNodeWithText("Hora del modelo:", substring = true).performScrollTo().fetchSemanticsNode()
        compose.onNodeWithText("Consulta:", substring = true).performScrollTo().fetchSemanticsNode()
        compose.onAllNodesWithText("APTA").assertCountEquals(0)
        compose.onNodeWithTag("weather-manual-confirmation").performScrollTo().assertIsOff()
    }

    @Test
    fun confirmacionMeteorologicaExigeVerificacionRealSeparadaDeValoresReferenciales() {
        var actualizada: PreChequeo? = null
        mostrar(onCambiar = { actualizada = it })

        compose.onAllNodesWithText("Mar y meteorología aptas", substring = true)
            .assertCountEquals(0)
        compose.onNodeWithTag("weather-manual-confirmation").assertIsOff()
        compose.onNodeWithTag("weather-manual-confirmation").performScrollTo().performClick()

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
        compose.onNodeWithText("Centro de Cultivo").performScrollTo().fetchSemanticsNode()
        compose.onNodeWithText("Fecha de Faena").performScrollTo().fetchSemanticsNode()
        compose.onNodeWithText("25/10/2026").performScrollTo().fetchSemanticsNode()
        compose.onNodeWithText("Turno de Marea").performScrollTo().fetchSemanticsNode()
        compose.onNodeWithText("Mañana (08:30 - 14:30)").performScrollTo().fetchSemanticsNode()
        compose.onAllNodes(hasText("Mañana (08:30 - 14:30)") and hasClickAction()).assertCountEquals(0)
        compose.onNodeWithText("Buzo Técnico Asignado").performScrollTo().fetchSemanticsNode()
        compose.onNodeWithText("Buzo Demo Alfa").performScrollTo().fetchSemanticsNode()
        compose.onAllNodesWithText("Supervisor de la sesión", substring = true)
            .assertCountEquals(0)
    }

    @Test
    fun selectoresMarcanLaOpcionActualYConservanLosDatosAlVolverASeleccionarla() {
        val centro = CENTROS_MARITIMOS[1]
        val otro = CENTROS_MARITIMOS.first()
        val apertura = aperturaDemo().copy(condicionesListas = true, edadBuzo = 35)
        var actualizada = apertura
        mostrar(apertura, onCambiar = { actualizada = it })

        compose.onNodeWithTag("centro-selector").performScrollTo().performClick()
        compose.onNode(hasText(centro.nombre) and isSelectable()).assertIsSelected().performClick()
        compose.runOnIdle { assertEquals(centro.id, actualizada.centroId) }
        compose.onNodeWithTag("centro-selector").performScrollTo().performClick()
        compose.onNode(hasText(otro.nombre) and isSelectable()).performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(otro.id, actualizada.centroId)
            assertEquals(false, actualizada.condicionesListas)
        }

        compose.onNodeWithTag("buzo-selector").performScrollTo().performClick()
        compose.onNode(hasText("Buzo Demo Alfa") and isSelectable()).assertIsSelected().performClick()
        compose.runOnIdle {
            assertEquals(35, actualizada.edadBuzo)
            assertEquals("BUZO-1", actualizada.buzoId)
        }

        compose.onNodeWithTag("buzo-selector").performScrollTo().performClick()
        compose.onNode(hasText("Buzo Demo Bravo") and isSelectable()).performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals("BUZO-2", actualizada.buzoId)
            assertEquals(0, actualizada.edadBuzo)
        }
    }

    @Test
    fun mayorDe45SinTestVigenteMuestraBloqueoYDeshabilitaContinuar() {
        mostrar(aperturaDemo().copy(
            edadBuzo = 50,
            testEsfuerzoVigente = false,
            vigenciaTestEsfuerzo = ""
        ))

        compose.onNodeWithText("Continuar al Checklist").assertIsNotEnabled()
        compose.onNodeWithText("se exige test de esfuerzo vigente", substring = true)
            .performScrollTo()
            .fetchSemanticsNode()
    }
}

private fun aperturaDemo() = PreChequeo(
    id = "demo",
    centroCultivo = "Centro Demo Nébula",
    fecha = "25/10/2026",
    hora = "09:30",
    buzoAsignado = "Buzo Demo Alfa",
    supervisorACargo = "Supervisor Demo"
)
