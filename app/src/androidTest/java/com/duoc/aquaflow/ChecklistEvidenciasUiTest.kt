package com.duoc.aquaflow

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.test.platform.app.InstrumentationRegistry
import com.duoc.aquaflow.data.local.EvidenciasFotos
import com.duoc.aquaflow.model.EstadoEquipo
import com.duoc.aquaflow.model.ItemEquipamiento
import com.duoc.aquaflow.model.MetadatosFoto
import com.duoc.aquaflow.ui.components.EvidenciasGaleria
import com.duoc.aquaflow.ui.screens.ChecklistScreenContent
import com.duoc.aquaflow.ui.theme.AquaFlowTheme
import org.junit.Rule
import org.junit.Assert.assertEquals
import org.junit.Test

class ChecklistEvidenciasUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun comparaSinAccionManualYAmpliaCapturaYReferencia() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val captura = EvidenciasFotos.crear(context)
        val referencia = EvidenciasFotos.crear(context)
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }
        try {
            for (uri in listOf(captura, referencia)) {
                requireNotNull(context.contentResolver.openOutputStream(uri)).use {
                    check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
                }
            }
            val ruta = captura.toString()
            compose.setContent {
                AquaFlowTheme {
                    EvidenciasGaleria(
                        fotos = listOf(ruta),
                        nombresPorFoto = mapOf(ruta to "Maleta"),
                        metadatos = mapOf(ruta to MetadatosFoto(referencia = referencia.toString()))
                    )
                }
            }
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText("Similitud visual orientativa: 100 %").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithContentDescription("Foto de Maleta").performClick()
            compose.onNodeWithText("Acercar").performClick()
            compose.onNodeWithContentDescription("Cerrar imagen").performClick()
            compose.onNodeWithText("Ampliar referencia").performClick()
            compose.onNodeWithText("Referencia · Maleta").assertExists()
            compose.onNodeWithContentDescription("Cerrar imagen").performClick()
        } finally {
            bitmap.recycle()
            EvidenciasFotos.descartar(context, captura)
            EvidenciasFotos.descartar(context, referencia)
        }
    }

    @Test
    fun permiteElegirReferenciaParaUnaFotoAntiguaSinMetadatos() {
        val foto = "content://aquaflow/foto-antigua"
        var seleccionada: String? = null
        compose.setContent {
            AquaFlowTheme {
                EvidenciasGaleria(fotos = listOf(foto), onElegirReferencia = { seleccionada = it })
            }
        }
        compose.onNodeWithText("Datos de captura sin registrar").assertExists()
        compose.onNodeWithText("Elegir referencia").performClick()
        compose.runOnIdle { assertEquals(foto, seleccionada) }
    }

    @Test
    fun evidenciaMuestraNombreYCategoriaDelEquipo() {
        val referencia = "content://aquaflow/evidencia-maleta"
        val equipo = ItemEquipamiento(
            id = "1",
            titulo = "Maleta de Suministro de Aire",
            descripcion = "Verificación del suministro primario.",
            estado = EstadoEquipo.CUMPLE,
            categoria = "EQUIPO 01 • SUMINISTRO PRIMARIO",
            badge = "FOTO OBLIGATORIA"
        )

        compose.setContent {
            AquaFlowTheme {
                ChecklistScreenContent(
                    ocupado = false,
                    error = null,
                    items = listOf(equipo),
                    fotos = listOf(referencia),
                    fotosPorEquipo = mapOf(equipo.id to referencia),
                    onEstadoCambiado = { _, _ -> },
                    onTomarFotoClick = {},
                    onQuitarFoto = {},
                    onGuardarClick = {},
                    onGuardarBorradorClick = {},
                    onVolver = {}
                )
            }
        }

        compose.onNodeWithText("Evidencia fotográfica capturada").assertExists()
        compose.onNodeWithText("EVIDENCIA 01").performScrollTo().assertExists()
        compose.onAllNodesWithText("Maleta de Suministro de Aire").assertCountEquals(2)
        compose.onAllNodesWithText("EQUIPO 01 • SUMINISTRO PRIMARIO").assertCountEquals(2)
    }

    @Test
    fun equipoRojoDeshabilitaContinuidadAunqueTengaEvidencia() {
        val referencia = "content://aquaflow/evidencia-maleta"
        val equipo = ItemEquipamiento("1", "Maleta de aire", "Crítica", EstadoEquipo.NO_CUMPLE)
        compose.setContent {
            AquaFlowTheme {
                ChecklistScreenContent(
                    false, null, listOf(equipo), listOf(referencia), mapOf("1" to referencia),
                    { _, _ -> }, {}, {}, {}, {}, {}
                )
            }
        }
        compose.onNodeWithText("Continuar a Salud").assertIsNotEnabled()
        compose.onNodeWithText("Equipo crítico sin conformidad: Maleta de aire.", substring = true).assertExists()
    }
}
