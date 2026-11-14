package com.duoc.aquaflow

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.duoc.aquaflow.model.EstadoEquipo
import com.duoc.aquaflow.model.ItemEquipamiento
import com.duoc.aquaflow.ui.screens.ChecklistScreenContent
import com.duoc.aquaflow.ui.theme.AquaFlowTheme
import org.junit.Rule
import org.junit.Test

class ChecklistEvidenciasUiTest {
    @get:Rule val compose = createComposeRule()

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
}
