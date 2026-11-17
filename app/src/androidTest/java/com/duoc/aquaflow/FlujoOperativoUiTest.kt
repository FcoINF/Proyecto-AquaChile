package com.duoc.aquaflow

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.duoc.aquaflow.model.*
import com.duoc.aquaflow.ui.screens.TareasScreen
import com.duoc.aquaflow.ui.screens.historial.DetalleFaenaScreen
import com.duoc.aquaflow.ui.theme.AquaFlowTheme
import com.duoc.aquaflow.viewmodel.ResumenFaenaViewModel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class FlujoOperativoUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun tareasExigenDescripcionYConfirmacionYPermitenBorrador() {
        var guardado: ChecklistTareas? = null
        var borrador: ChecklistTareas? = null
        compose.setContent { AquaFlowTheme { TareasScreen("prueba", null, false, null, { guardado = it }, { borrador = it }, {}) } }
        compose.onNodeWithText("Guardar tareas y revisar").performClick()
        compose.onNodeWithText("Registra y verifica", substring = true).assertExists()
        compose.onNodeWithText("Agregar tarea").performScrollTo().performClick()
        compose.onNodeWithText("Tarea 1").performScrollTo().performTextInput("Inspección de red")
        compose.onNodeWithText("Guardar borrador").performClick()
        compose.runOnIdle { assertEquals(EstadoSeccion.BORRADOR, borrador!!.estado); assertNull(guardado) }
        compose.onNode(isToggleable()).performScrollTo().performClick()
        compose.onNodeWithText("Guardar tareas y revisar").performClick()
        compose.runOnIdle { assertEquals(EstadoSeccion.COMPLETA, guardado!!.estado); assertTrue(guardado!!.items.single().verificada) }
    }

    @Test fun inicioSoloTrasAutorizacionYCierreSoloTrasInicio() {
        val r = ResumenFaena(
            PreChequeo("f1", "Centro", "26/10/2026", "10:00", "Ana", "supervisor", true, true, true),
            ChecklistFaena("f1", listOf(ItemEquipamiento("1", "Equipo", "", EstadoEquipo.CUMPLE)), listOf("foto")),
            FormularioAst("Limpieza", false, List(PREGUNTAS_AST.size) { true }, emptyList(), ResultadoAst.CUMPLE),
            EncuestaSalud(120, 80, 98, emptyList(), NivelSalud.NORMAL), tareasCompletas())
        val faena = mutableStateOf(FaenaGuardada(r))
        val vm = ResumenFaenaViewModel()
        compose.setContent { AquaFlowTheme {
            DetalleFaenaScreen(vm, faena.value, Rol.SUPERVISOR, "supervisor", {}, {}, {}, onCerrar = {},
                onIniciar = { faena.value = faena.value.copy(inicio = RegistroInicio("supervisor", 1)) })
        } }
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Volver al historial"))
        compose.onNodeWithText("Cierre faena").assertDoesNotExist()
        compose.onNodeWithText("Cerrar faena").assertDoesNotExist()
        compose.runOnIdle { faena.value = faena.value.copy(decision = RegistroDecision(Decision.AUTORIZADA, "", "jefe")) }
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Cierre faena"))
        compose.onNodeWithText("Cierre faena").assertIsEnabled().performClick()
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Cerrar faena"))
        compose.onNodeWithText("Cerrar faena").assertExists()
        compose.onNodeWithText("Presión sistólica de salida").assertExists()
        compose.onNodeWithText("Cierre faena").assertDoesNotExist()
    }

    @Test fun resultadoRojoDeshabilitaAutorizarPeroPermiteRechazar() {
        val resumen = ResumenFaena(
            PreChequeo("f-roja", "Centro", "26/10/2026", "10:00", "Ana", "supervisor", true, true, true),
            ChecklistFaena(
                "f-roja",
                listOf(ItemEquipamiento("1", "Maleta de aire", "", EstadoEquipo.NO_CUMPLE)),
                listOf("foto"),
                fotosPorEquipo = mapOf("1" to "foto")
            ),
            FormularioAst("Limpieza", false, List(PREGUNTAS_AST.size) { true }, emptyList(), ResultadoAst.CUMPLE),
            EncuestaSalud(120, 80, 98, emptyList(), NivelSalud.NORMAL),
            tareasCompletas()
        )
        compose.setContent {
            AquaFlowTheme {
                DetalleFaenaScreen(ResumenFaenaViewModel(), FaenaGuardada(resumen), Rol.JEFE, "jefe", {}, {}, {})
            }
        }
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Autorizar"))
        compose.onNodeWithText("Autorizar").assertIsNotEnabled()
        compose.onNodeWithText("Rechazar").assertIsEnabled()
        compose.onNodeWithText("Una justificación no puede omitir", substring = true).assertExists()
    }
}
