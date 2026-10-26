package com.duoc.aquaflow

import com.duoc.aquaflow.data.repository.PreChequeoRepository
import com.duoc.aquaflow.data.repository.ConsultaAperturas
import com.duoc.aquaflow.domain.GrupoFaena
import com.duoc.aquaflow.domain.ReglasPanel
import com.duoc.aquaflow.model.*
import com.duoc.aquaflow.viewmodel.DashboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class PanelAdministradorTest {
    private val supervisor = Usuario("Luis", "1234", Rol.SUPERVISOR)
    private val jefe = Usuario("jefe", "1234", Rol.JEFE)
    private val apertura = PreChequeo("f1", "Centro Norte", "26/10/2026", "10:00", "Ana", "Luis", true, true, true)
    private val resumen = ResumenFaena(apertura,
        ChecklistFaena("f1", listOf(ItemEquipamiento("1", "Equipo", "", EstadoEquipo.CUMPLE))),
        FormularioAst("Limpieza", false, List(PREGUNTAS_AST.size) { true }, emptyList(), ResultadoAst.CUMPLE),
        EncuestaSalud(120, 80, 98, emptyList(), NivelSalud.NORMAL))
    private val abierta = FaenaGuardada(resumen)
    private val autorizada = abierta.copy(decision = RegistroDecision(Decision.AUTORIZADA, "", "jefe"))
    private val iniciada = autorizada.copy(inicio = RegistroInicio("Luis", 1L))
    private val rechazada = abierta.copy(decision = RegistroDecision(Decision.RECHAZADA, "Revisar equipo", "jefe"))
    private val cerrada = iniciada.copy(cierre = RegistroCierre(30, "Sin novedades", true, "Luis"))

    @Before fun preparar() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun terminar() { Dispatchers.resetMain() }

    @Test fun cuatroGruposExcluyenBorradoresYNoDuplicanCerradas() {
        val borrador = abierta.copy(resumen = resumen.copy(ast = null))
        val grupos = ReglasPanel.agrupar(listOf(borrador, abierta, autorizada, iniciada, rechazada, cerrada))
        assertEquals(listOf(abierta), grupos[GrupoFaena.ABIERTAS])
        assertEquals(listOf(autorizada, iniciada), grupos[GrupoFaena.EN_PROCESO])
        assertEquals(listOf(rechazada), grupos[GrupoFaena.RECHAZADAS])
        assertEquals(listOf(cerrada), grupos[GrupoFaena.CERRADAS])
        assertFalse(ReglasPanel.visible(borrador, Rol.JEFE))
        assertTrue(ReglasPanel.visible(borrador, Rol.SUPERVISOR))
    }

    @Test fun formularioGuardadoComoBorradorNoEsFaenaAbierta() {
        val borrador = abierta.copy(resumen = resumen.copy(encuesta = resumen.encuesta!!.copy(estado = EstadoSeccion.BORRADOR)))
        assertNull(ReglasPanel.grupo(borrador))
        assertEquals(4, ReglasPanel.agrupar(emptyList()).size)
        assertTrue(ReglasPanel.agrupar(emptyList()).values.all { it.isEmpty() })
    }

    @Test fun expedienteConDecisionSigueVisibleAunqueSeaDeVersionAnterior() {
        assertEquals(GrupoFaena.EN_PROCESO, ReglasPanel.grupo(autorizada.copy(resumen = resumen.copy(ast = null))))
    }

    @Test fun errorDeLecturaSeDistingueDeListaVaciaYPermiteReintentar() = runTest {
        var fallar = true
        val consulta = object : ConsultaAperturas {
            override fun observar(usuario: Usuario): Flow<List<PreChequeo>> = flowOf(emptyList())
            override fun observarDetalle(id: String, usuario: Usuario): Flow<PreChequeo?> = flowOf(null)
            override fun observarFaenas(usuario: Usuario): Flow<List<FaenaGuardada>> = flow {
                check(!fallar)
                emit(emptyList())
            }
        }
        val vm = DashboardViewModel(consulta, jefe)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect() }
        advanceUntilIdle()
        assertNotNull(vm.state.value.error)
        assertFalse(vm.state.value.cargando)
        fallar = false
        vm.reintentar()
        advanceUntilIdle()
        assertNull(vm.state.value.error)
        assertFalse(vm.state.value.cargando)
        assertTrue(vm.state.value.faenas.isEmpty())
    }

    @Test fun roomPublicaSoloFaenasCompletasAlJefeYConservaBorradoresDelSupervisor() = runTest {
        val repo = PreChequeoRepository(DaoMemoria())
        val vm = DashboardViewModel(repo, jefe)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect() }
        repo.guardar(apertura)
        advanceUntilIdle()
        assertFalse(vm.state.value.cargando)
        assertTrue(vm.state.value.faenas.isEmpty())
        assertNull(repo.observarFaena("f1", jefe).first())
        assertEquals(1, repo.observarFaenas(supervisor).first().size)
        repo.guardarChecklist("f1", resumen.checklist!!)
        repo.guardarAst("f1", resumen.ast!!)
        repo.guardarEncuesta("f1", resumen.encuesta!!)
        advanceUntilIdle()
        assertEquals(GrupoFaena.ABIERTAS, ReglasPanel.grupo(vm.state.value.faenas.single()))
        repo.decidir("f1", RegistroDecision(Decision.AUTORIZADA, "", "jefe"), jefe)
        advanceUntilIdle()
        assertEquals(GrupoFaena.EN_PROCESO, ReglasPanel.grupo(vm.state.value.faenas.single()))
        repo.iniciar("f1", supervisor)
        advanceUntilIdle()
        assertEquals(EstadoFaena.EN_EJECUCION, vm.state.value.faenas.single().estado)
        repo.cerrar("f1", RegistroCierre(30, "Sin novedades", true, saludSalida = saludSalida()), supervisor)
        advanceUntilIdle()
        assertEquals(GrupoFaena.CERRADAS, ReglasPanel.grupo(vm.state.value.faenas.single()))
        assertEquals("Luis", vm.state.value.faenas.single().cierre!!.cerradoPor)
    }
}
