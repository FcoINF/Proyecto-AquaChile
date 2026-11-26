package com.duoc.aquaflow

import android.content.Context
import com.duoc.aquaflow.data.remote.*
import com.duoc.aquaflow.data.repository.*
import com.duoc.aquaflow.model.*
import com.duoc.aquaflow.viewmodel.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class FlujosViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val supervisor = Usuario("supervisor", "", Rol.SUPERVISOR)
    private val jefe = Usuario("jefe", "", Rol.JEFE)
    private val apertura = PreChequeo("faena-1", "Centro", "26/10/2026", "09:00", "Ana", "supervisor", true, true, true)
    private val id = apertura.id
    private val resumen = ResumenFaena(
        apertura,
        ChecklistFaena(id, emptyList()),
        FormularioAst("Inspección", false, List(5) { true }, emptyList(), ResultadoAst.CUMPLE),
        EncuestaSalud(120, 80, 98, emptyList(), NivelSalud.NORMAL),
        tareasCompletas()
    )
    private val faena = FaenaGuardada(resumen)
    private val cierre = RegistroCierre(20, "Revisión completada", true)
    private val decision = RegistroDecision(Decision.AUTORIZADA, "Revisado", jefe.nombre)

    @Before fun configurar() { Dispatchers.setMain(dispatcher) }
    @After fun restaurar() { Dispatchers.resetMain() }

    private fun consulta(usuario: Usuario, local: FaenaGuardada?) = mockk<ConsultaAperturas> {
        every { observarFaena(id, usuario) } returns flowOf(local)
    }

    @Test fun jefaturaDecideSobreLaVersionVistaYRegistraLaRevisionDeAlertas() = runTest {
        val contexto = mockk<Context>()
        val remoto = mockk<FaenasRemotas>()
        val expediente = DetalleRemoto(faena, 4, null, emptyList(), true)
        coEvery { remoto.detalle(contexto, id) } returns expediente
        coEvery { remoto.decidir(id, 4, decision) } just Runs
        coEvery { remoto.revisarAlerta(id, "ASCENSO") } throws IllegalStateException("Revisión rechazada")
        val vm = DetalleAperturaViewModel(consulta(jefe, null), jefe, id, remoto = remoto, contexto = contexto)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect() }
        runCurrent()
        assertEquals(4L, vm.state.value.expedienteRemoto?.version)
        vm.auditarAutorizacion("Revisado")
        runCurrent()
        coVerify(exactly = 1) { remoto.decidir(id, 4, decision) }
        vm.revisarAlerta("ASCENSO")
        runCurrent()
        assertEquals("Revisión rechazada", vm.state.value.errorDecision)
        assertFalse(vm.state.value.guardando)
        coVerify(atLeast = 2) { remoto.detalle(contexto, id) }
    }

    @Test fun supervisorReservaElExpedienteYCopiaLasSeccionesAntesDeEditar() = runTest {
        val contexto = mockk<Context>()
        val remoto = mockk<FaenasRemotas>(relaxUnitFun = true)
        val repo = mockk<PreChequeoRepository>(relaxUnitFun = true)
        coEvery { remoto.detalle(contexto, id) } returns DetalleRemoto(faena, 7, null, emptyList(), true)
        coEvery { repo.recuperarFaenaPorId(id) } returns null
        val vm = DetalleAperturaViewModel(consulta(supervisor, null), supervisor, id, remoto = remoto, contexto = contexto)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect() }
        runCurrent()
        var abrio = false
        vm.prepararEdicion(repo) { abrio = true }
        runCurrent()
        assertTrue(abrio)
        coVerifyOrder {
            remoto.reservarEdicion(id, 7)
            repo.guardar(apertura)
            repo.guardarChecklist(id, requireNotNull(resumen.checklist))
            repo.guardarAst(id, requireNotNull(resumen.ast))
            repo.guardarEncuesta(id, requireNotNull(resumen.encuesta))
            repo.guardarTareas(id, requireNotNull(resumen.tareas))
        }
        vm.iniciar()
        runCurrent()
        vm.cerrar(cierre)
        runCurrent()
        vm.retirar()
        runCurrent()
        coVerify(exactly = 1) { remoto.iniciar(id) }
        coVerify(exactly = 1) { remoto.cerrar(id, match { it.cerradoEn > 0 && it.observaciones == cierre.observaciones }) }
        coVerify(exactly = 1) { remoto.retirar(id, 7) }
    }

    @Test fun conservaBorradorSinRedYExigeConexionAlEditarUnExpedienteSincronizado() = runTest {
        val contexto = mockk<Context>()
        val remoto = mockk<FaenasRemotas>()
        val repo = mockk<PreChequeoRepository>()
        coEvery { remoto.detalle(contexto, id) } throws IllegalStateException("Sin conexión")
        val vm = DetalleAperturaViewModel(consulta(supervisor, faena), supervisor, id, remoto = remoto, contexto = contexto)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect() }
        runCurrent()
        assertEquals(faena, vm.state.value.faena)
        assertEquals("Sin conexión", vm.state.value.error)
        vm.prepararEdicion(repo, sincronizado = true) { fail("No debe abrir una edición sin conexión") }
        runCurrent()
        assertTrue(vm.state.value.errorDecision.orEmpty().contains("Conéctate"))
        coEvery { remoto.detalle(contexto, id) } throws IllegalStateException("No encontrado",
            HttpException(Response.error<String>(404, "".toResponseBody())))
        vm.reintentar()
        runCurrent()
        assertNull(vm.state.value.error)
        assertEquals(faena, vm.state.value.faena)
    }

    @Test fun inicioYCierreLocalPropaganRechazosYEvitanUnaOperacionParalela() = runTest {
        val servicio = mockk<CierreFaenas>()
        val inicio = CompletableDeferred<Unit>()
        coEvery { servicio.iniciar(id, supervisor) } coAnswers { inicio.await() }
        coEvery { servicio.cerrar(id, cierre, supervisor) } throws IllegalStateException("Falta revisión de salida")
        val vm = DetalleAperturaViewModel(consulta(supervisor, faena), supervisor, id, cierres = servicio)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect() }
        vm.iniciar()
        runCurrent()
        assertTrue(vm.state.value.guardando)
        vm.cerrar(cierre)
        coVerify(exactly = 0) { servicio.cerrar(any(), any(), any()) }
        inicio.complete(Unit)
        runCurrent()
        vm.cerrar(cierre)
        runCurrent()
        assertFalse(vm.state.value.guardando)
        assertTrue(vm.state.value.errorDecision.orEmpty().contains("Falta revisión de salida"))
        coEvery { servicio.iniciar(id, supervisor) } throws IllegalStateException("Falta autorización")
        vm.iniciar()
        runCurrent()
        assertTrue(vm.state.value.errorDecision.orEmpty().contains("Falta autorización"))
    }

    @Test fun listadoConservaLosExpedientesSiFallaLaActualizacionYPermiteReintentar() = runTest {
        val remoto = mockk<FaenasRemotas>()
        val aperturaRemota = AperturaRemota(id, supervisor.nombre, AperturaDatos(apertura))
        val respuesta = CompletableDeferred<List<AperturaRemota>>()
        coEvery { remoto.listar() } coAnswers { respuesta.await() }
        val vm = AperturasRecibidasViewModel(remoto)
        assertTrue(vm.state.value.cargando)
        vm.actualizar()
        respuesta.complete(listOf(aperturaRemota))
        runCurrent()
        coVerify(exactly = 1) { remoto.listar() }
        coEvery { remoto.listar() } throws IllegalStateException()
        vm.actualizar()
        runCurrent()
        assertEquals(listOf(aperturaRemota), vm.state.value.aperturas)
        assertEquals("No se pudieron consultar las aperturas.", vm.state.value.error)
        coEvery { remoto.listar() } returns emptyList()
        vm.actualizar()
        runCurrent()
        assertNull(vm.state.value.error)
        assertTrue(vm.state.value.aperturas.isEmpty())
    }

    @Test fun ingresoRemotoIniciaSincronizacionDeSupervisorYDejaReintentarTrasUnError() = runTest {
        val remoto = mockk<FaenasRemotas>(relaxUnitFun = true)
        val sincronizacion = mockk<SincronizacionFaenas>(relaxed = true)
        coEvery { remoto.ingresar("supervisor", "clave", Rol.SUPERVISOR) } throws IllegalStateException()
        val vm = SesionViewModel(remoto, sincronizacion)
        var navegaciones = 0
        vm.ingresar { navegaciones++ }
        assertNotNull(vm.ingreso.value.error)
        vm.cambiarNombre(" supervisor ")
        vm.cambiarClave("clave")
        vm.ingresar { navegaciones++ }
        runCurrent()
        assertNull(vm.usuario.value)
        assertEquals("No se pudo iniciar sesión.", vm.ingreso.value.error)
        assertFalse(vm.ingresar())
        assertFalse(vm.ingresar("supervisor", "clave"))
        coEvery { remoto.ingresar("supervisor", "clave", Rol.SUPERVISOR) } returns supervisor
        vm.ingresar { navegaciones++ }
        vm.ingresar { navegaciones++ }
        runCurrent()
        assertEquals(1, navegaciones)
        assertEquals(supervisor, vm.usuario.value)
        coVerify(exactly = 1) { sincronizacion.iniciarSesion(supervisor.nombre, remoto) }
        vm.salir()
        assertNull(vm.usuario.value)
        verify(exactly = 1) { sincronizacion.cerrarSesion() }
        verify(exactly = 1) { remoto.salir() }
    }

    @Test fun resumenAvisaElErrorYCompletaUnSoloEnvioAlReintentar() = runTest {
        val vm = ResumenFaenaViewModel()
        var completados = 0
        vm.enviar({ throw IllegalStateException() }) { completados++ }
        runCurrent()
        assertEquals("No se pudo enviar el expediente.", vm.ui.value.error)
        assertFalse(vm.ui.value.enviando)
        val envio = CompletableDeferred<Unit>()
        vm.enviar({ envio.await() }) { completados++ }
        vm.enviar({ fail("El envío anterior sigue activo") }) { completados++ }
        runCurrent()
        assertTrue(vm.ui.value.enviando)
        envio.complete(Unit)
        runCurrent()
        assertEquals(1, completados)
        assertFalse(vm.ui.value.enviando)
        assertNull(vm.ui.value.error)
    }

    @Test fun borradorInexistenteYBloqueoPorTestVencidoSeReportanAlSupervisor() = runTest {
        val borradores = mockk<BorradoresApertura>(relaxUnitFun = true)
        val registro = mockk<RegistroFaena>(relaxUnitFun = true)
        coEvery { borradores.recuperarFaenaPorId(id) } returns null
        val vm = FaenaViewModel(borradores, registro)
        vm.iniciar(supervisor)
        vm.cargarFaena(id)
        runCurrent()
        assertEquals("No se encontró el borrador.", vm.ui.value.error)
        vm.actualizar(apertura)
        assertTrue(vm.confirmarApertura())
        assertTrue(vm.registrarTareas(tareasCompletas()))
        runCurrent()
        vm.guardarBorradorTareas(tareasCompletas())
        runCurrent()
        coVerify { registro.guardarTareas(any(), match { it.estado == EstadoSeccion.BORRADOR }) }
        vm.actualizar(apertura.copy(edadBuzo = 46, testEsfuerzoVigente = false))
        vm.guardarApertura(continuar = true) { fail("La apertura con test vencido debe bloquearse") }
        runCurrent()
        coVerify { borradores.registrarBloqueo(any(), match { it.accion == "Confirmar apertura" }) }
        assertFalse(vm.ui.value.aperturaConfirmada)
    }

    @Test fun astRecuperadoConservaLasEdicionesYGuardaLosPasosNormalizados() {
        val vm = AstViewModel()
        val original = requireNotNull(resumen.ast)
        vm.cargar(original)
        vm.elegirTipologia("Mantención")
        vm.alternarNuevoAst()
        vm.agregarPaso()
        vm.agregarPaso()
        vm.cambiarPaso(0, PasoAst(" Revisar ", " Corte ", " Guantes ", false))
        vm.quitarPaso(1)
        vm.responder(0, false)
        assertEquals(ResultadoAst.REQUIERE_REVISION, vm.ui.value.resultado)
        assertNull(vm.guardar())
        vm.responder(0, true)
        vm.cargar(original)
        val guardado = requireNotNull(vm.guardar())
        assertEquals("Mantención", guardado.tipologia)
        assertEquals(PasoAst("Revisar", "Corte", "Guantes", false), guardado.pasos.single())
        assertEquals(EstadoSeccion.BORRADOR, vm.borrador().estado)
    }

    @Test fun encuestaRecuperadaConservaCambiosYMarcaMedicionesFueraDeRango() {
        val vm = EncuestaSaludViewModel()
        val original = requireNotNull(resumen.encuesta)
        vm.cargar(original)
        vm.cambiarSistolica("999")
        vm.cambiarDiastolica("999")
        vm.cambiarSaturacion("999")
        vm.cargar(original)
        assertTrue(vm.ui.value.sistolicaFuera)
        assertTrue(vm.ui.value.diastolicaFuera)
        assertTrue(vm.ui.value.saturacionFuera)
        assertNull(vm.guardar())
        assertEquals(EstadoSeccion.BORRADOR, vm.borrador().estado)
        val pregunta = PREGUNTAS_SALUD.first()
        vm.responderPregunta(pregunta, true)
        vm.alternarAlerta(pregunta)
        assertTrue(pregunta in vm.ui.value.alertas)
        vm.alternarAlerta(pregunta)
        assertFalse(pregunta in vm.ui.value.alertas)
    }

    @Test fun errorDeLecturaYErrorDeAuditoriaQuedanVisiblesEnDetalle() = runTest {
        val lectura = mockk<ConsultaAperturas> {
            every { observarFaena(id, jefe) } returns flow { throw IllegalStateException() }
        }
        val registro = mockk<RegistroFaena>()
        coEvery { registro.auditarAutorizacion(id, "Observación", jefe) } throws IllegalStateException("Room ocupado")
        val vm = DetalleAperturaViewModel(lectura, jefe, id, registro = registro)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect() }
        runCurrent()
        assertEquals("No se pudo cargar la apertura.", vm.state.value.error)
        vm.auditarAutorizacion("Observación")
        runCurrent()
        assertTrue(vm.state.value.errorDecision.orEmpty().contains("Room ocupado"))
        assertFalse(vm.state.value.guardando)
    }
}
