package com.duoc.aquaflow

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.duoc.aquaflow.model.*
import com.duoc.aquaflow.ui.screens.historial.DetalleFaenaScreen
import com.duoc.aquaflow.ui.theme.AquaFlowTheme
import com.duoc.aquaflow.viewmodel.ResumenFaenaViewModel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DetalleIncompletoUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun historialDeDevelopBloqueaAmbasDecisionesHastaConfirmarBorrador() {
        val c = ChecklistFaena("f1", listOf(ItemEquipamiento("1", "Equipo", "", EstadoEquipo.CUMPLE)))
        val r = mutableStateOf(ResumenFaena(
            PreChequeo("f1", "Centro", "25/10/2026", "10:00", "Ana", "Luis", true, true, true),
            c.copy(estado = EstadoSeccion.BORRADOR),
            FormularioAst("Limpieza", false, List(PREGUNTAS_AST.size) { true }, emptyList(), ResultadoAst.CUMPLE),
            EncuestaSalud(120, 80, 98, emptyList(), NivelSalud.NORMAL), tareasCompletas()
        ))
        val vm = ResumenFaenaViewModel()
        var enviada: RegistroDecision? = null
        compose.setContent { AquaFlowTheme {
            DetalleFaenaScreen(vm, FaenaGuardada(r.value), Rol.JEFE, "jefe", { enviada = it }, {}, {})
        } }
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Estado: Borrador", substring = true))
        compose.onNodeWithText("Estado: Borrador", substring = true).assertIsDisplayed()
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Autorizar"))
        compose.onNodeWithText("Autorizar").assertIsNotEnabled()
        compose.onNodeWithText("Rechazar").assertIsNotEnabled()
        compose.runOnIdle { assertNull(enviada); r.value = r.value.copy(checklist = c) }
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Autorizar"))
        compose.onNodeWithText("Autorizar").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(Decision.AUTORIZADA, enviada!!.decision) }
    }
}
