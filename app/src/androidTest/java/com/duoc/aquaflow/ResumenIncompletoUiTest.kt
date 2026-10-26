package com.duoc.aquaflow

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.duoc.aquaflow.model.*
import com.duoc.aquaflow.ui.screens.ResumenFaenaScreen
import com.duoc.aquaflow.ui.theme.AquaFlowTheme
import com.duoc.aquaflow.viewmodel.ResumenFaenaViewModel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ResumenIncompletoUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun ambosBotonesSeHabilitanSoloAlCompletarElExpediente() {
        val c = ChecklistFaena("f1", listOf(ItemEquipamiento("1", "Equipo", "", EstadoEquipo.CUMPLE)))
        val resumen = mutableStateOf(ResumenFaena(
            PreChequeo("f1", "Centro", "25/10/2026", "10:00", "Ana", "Luis", true, true, true),
            c.copy(estado = EstadoSeccion.BORRADOR),
            FormularioAst("Limpieza", false, List(PREGUNTAS_AST.size) { true }, listOf(PasoAst("Tarea", "Peligro", "Control")), ResultadoAst.CUMPLE),
            EncuestaSalud(120, 80, 98, emptyList(), NivelSalud.NORMAL)
        ))
        var enviada: RegistroDecision? = null
        val vm = ResumenFaenaViewModel()
        compose.setContent { AquaFlowTheme {
            ResumenFaenaScreen(vm, resumen.value, null, Rol.JEFE, "jefe", { enviada = it }, {}, {})
        } }
        compose.onNodeWithText("Estado: Borrador").assertExists()
        compose.onNodeWithText("Autorizar").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Rechazar").assertIsNotEnabled()
        compose.runOnIdle { assertNull(enviada); resumen.value = resumen.value.copy(checklist = c) }
        compose.onNodeWithText("Autorizar").assertIsEnabled()
        compose.onNodeWithText("Rechazar").assertIsEnabled()
        compose.onNodeWithText("Autorizar").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(Decision.AUTORIZADA, enviada!!.decision) }
    }
}
