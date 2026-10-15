package com.duoc.aquaflow

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.duoc.aquaflow.data.local.AquaFlowDatabase
import com.duoc.aquaflow.data.seed.DatabaseSeeder
import com.duoc.aquaflow.domain.AppError
import com.duoc.aquaflow.domain.AppResult
import com.duoc.aquaflow.domain.catalog.Catalogos
import com.duoc.aquaflow.domain.getOrNull
import com.duoc.aquaflow.model.AntecedentesAst
import com.duoc.aquaflow.model.Buzo
import com.duoc.aquaflow.model.CentroCultivo
import com.duoc.aquaflow.model.EncuestaSalud
import com.duoc.aquaflow.model.EstadoEquipo
import com.duoc.aquaflow.model.EstadoFaena
import com.duoc.aquaflow.model.RespuestaChecklist
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.model.TipoEvento
import com.duoc.aquaflow.model.TipoEvidencia
import com.duoc.aquaflow.model.Usuario
import com.duoc.aquaflow.repository.EvidenciaArchivos
import com.duoc.aquaflow.repository.RoomFaenaRepository
import com.duoc.aquaflow.repository.RoomUsuarioRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

private class ArchivosFalsos : EvidenciaArchivos {
    val eliminados = mutableListOf<String>()
    override fun eliminar(rutaRelativa: String) {
        eliminados += rutaRelativa
    }
}

/**
 * Pruebas con una base Room real (en memoria): flujo completo, permisos, transiciones y persistencia.
 * Requieren un dispositivo o emulador (Gradle: connectedDebugAndroidTest).
 */
@RunWith(AndroidJUnit4::class)
class FaenaRepositoryInstrumentedTest {

    private lateinit var context: Context
    private lateinit var db: AquaFlowDatabase
    private lateinit var repo: RoomFaenaRepository
    private val archivos = ArchivosFalsos()

    private lateinit var supervisor: Usuario
    private lateinit var otroSupervisor: Usuario
    private lateinit var admin: Usuario

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AquaFlowDatabase::class.java).allowMainThreadQueries().build()
        runBlocking {
            DatabaseSeeder(db).ensureSeeded()
            supervisor = db.usuarioDao().findByRut("15892403-K")!!
            otroSupervisor = db.usuarioDao().findByRut("14785236-3")!!
            admin = db.usuarioDao().findByRut("12433910-3")!!
        }
        repo = RoomFaenaRepository(db, archivos)
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ---------------------------------------------------------------- helpers

    private suspend fun centro(): CentroCultivo = db.centroDao().observeActivos().first().first()

    private suspend fun buzoVigente(indice: Int = 0): Buzo {
        val hoy = LocalDate.now().toEpochDay()
        return db.buzoDao().observeActivos().first().filter { it.certificacionVigenteHastaEpochDay > hoy }[indice]
    }

    private suspend fun buzoVencido(): Buzo {
        val hoy = LocalDate.now().toEpochDay()
        return db.buzoDao().observeActivos().first().first { it.certificacionVigenteHastaEpochDay < hoy }
    }

    private fun enUnaHora() = System.currentTimeMillis() + 3_600_000L

    private suspend fun nuevaFaena(buzo: Buzo? = null): Long {
        val elegido = buzo ?: buzoVigente()
        return repo.crearFaena(supervisor, centro().id, elegido.id, enUnaHora()).getOrNull()!!
    }

    private suspend fun completarPrechequeo(
        id: Long,
        estadoEquipo: EstadoEquipo = EstadoEquipo.CUMPLE,
        notaEquipo: String = "",
        gatingApto: Boolean = true
    ) {
        assertTrue(repo.guardarChecklist(supervisor, id, Catalogos.equipos.map { RespuestaChecklist(id, it.id, estadoEquipo, notaEquipo) }) is AppResult.Ok)
        val ast = AntecedentesAst(
            faenaId = id,
            tipologia = "Inspección de fondo",
            tarea = "Revisión de fondeos",
            pasos = "1. Descenso\n2. Ascenso",
            profundidadMaxPlanificadaM = 20.0,
            actualizadoAt = 0
        )
        assertTrue(
            repo.guardarAst(
                supervisor, id, ast,
                Catalogos.gatingAst.associate { it.id to gatingApto },
                Catalogos.riesgosAst.associate { it.id to true }
            ) is AppResult.Ok
        )
        val encuesta = EncuestaSalud(faenaId = id, sistolica = 120, diastolica = 80, saturacionO2 = 98, temperaturaC = 36.6, actualizadoAt = 0)
        assertTrue(
            repo.guardarSalud(supervisor, id, encuesta, Catalogos.declaracionSalud.associate { it.id to true }) is AppResult.Ok
        )
    }

    private suspend fun estadoDe(id: Long): EstadoFaena = db.faenaDao().findById(id)!!.estado

    private fun error(resultado: AppResult<*>): AppError = (resultado as AppResult.Fail).error

    // ---------------------------------------------------------------- flujo

    @Test
    fun flujoCompleto_desdeLaAperturaHastaElCierreYElHistorial() = runBlocking {
        val id = nuevaFaena()
        assertEquals(EstadoFaena.PRECHEQUEO, estadoDe(id))
        assertTrue(db.faenaDao().findById(id)!!.codigo.startsWith("F-"))

        completarPrechequeo(id)
        assertTrue(repo.enviarARevision(supervisor, id) is AppResult.Ok)
        assertEquals(EstadoFaena.PENDIENTE_AUTORIZACION, estadoDe(id))
        assertNotNull(db.faenaDao().findById(id)!!.supervisorFirmaAt)

        assertTrue(repo.autorizar(admin, id) is AppResult.Ok)
        assertEquals(EstadoFaena.AUTORIZADA, estadoDe(id))
        assertEquals(admin.id, db.faenaDao().findById(id)!!.decisionPorId)

        val cierre = repo.registrarPosterior(supervisor, id, 38, 22.4, 110, true, true, "Sin novedades")
        assertTrue("El cierre falló: $cierre", cierre is AppResult.Ok)
        assertEquals(EstadoFaena.CERRADA, estadoDe(id))

        val detalle = repo.observarDetalle(admin, id).first()!!
        assertEquals(38, detalle.registro!!.tiempoInmersionMin)
        assertEquals(Catalogos.equipos.size, detalle.checklist.size)
        assertEquals(
            listOf(
                TipoEvento.APERTURA, TipoEvento.CHECKLIST_GUARDADO, TipoEvento.AST_GUARDADO, TipoEvento.SALUD_GUARDADA,
                TipoEvento.ENVIADA_A_REVISION, TipoEvento.AUTORIZADA, TipoEvento.CERRADA
            ),
            detalle.eventos.map { it.tipo }
        )
        // El historial del supervisor y del administrador muestra la faena cerrada.
        assertEquals(EstadoFaena.CERRADA, repo.observarFaenas(supervisor).first().single().estado)
        assertEquals(1, repo.observarFaenas(admin).first().size)
    }

    @Test
    fun noSePuedeAutorizarConUnEquipoQueNoCumple_peroSiRechazarConMotivo() = runBlocking {
        val id = nuevaFaena()
        completarPrechequeo(id, estadoEquipo = EstadoEquipo.NO_CUMPLE, notaEquipo = "Sin presión")
        assertTrue(repo.enviarARevision(supervisor, id) is AppResult.Ok)

        assertTrue(error(repo.autorizar(admin, id)) is AppError.Blocked)
        assertEquals(EstadoFaena.PENDIENTE_AUTORIZACION, estadoDe(id))

        assertTrue(error(repo.rechazar(admin, id, "corto")) is AppError.Validation)
        assertEquals(EstadoFaena.PENDIENTE_AUTORIZACION, estadoDe(id))

        assertTrue(repo.rechazar(admin, id, "Equipos críticos sin presión de trabajo") is AppResult.Ok)
        val faena = db.faenaDao().findById(id)!!
        assertEquals(EstadoFaena.RECHAZADA, faena.estado)
        assertEquals("Equipos críticos sin presión de trabajo", faena.decisionMotivo)
    }

    @Test
    fun unaRespuestaNOenElGatingDelAstBloqueaLaAutorizacion() = runBlocking {
        val id = nuevaFaena()
        completarPrechequeo(id, gatingApto = false)
        assertTrue(repo.enviarARevision(supervisor, id) is AppResult.Ok)
        assertTrue(error(repo.autorizar(admin, id)) is AppError.Blocked)
    }

    @Test
    fun noSePuedeEnviarARevisionConFormulariosIncompletos() = runBlocking {
        val id = nuevaFaena()
        val resultado = repo.enviarARevision(supervisor, id)
        assertTrue(error(resultado) is AppError.Blocked)
        assertEquals(EstadoFaena.PRECHEQUEO, estadoDe(id))
    }

    @Test
    fun despuesDeEnviarLosFormulariosQuedanBloqueados() = runBlocking {
        val id = nuevaFaena()
        completarPrechequeo(id)
        assertTrue(repo.enviarARevision(supervisor, id) is AppResult.Ok)

        val cambio = repo.guardarChecklist(supervisor, id, listOf(RespuestaChecklist(id, "EQ_TRAJE", EstadoEquipo.NO_CUMPLE, "x")))
        assertEquals(AppError.Forbidden, error(cambio))
        assertEquals(AppError.Forbidden, error(repo.enviarARevision(supervisor, id)))
    }

    // ---------------------------------------------------------------- permisos y transiciones

    @Test
    fun soloUnSupervisorPuedeAbrirFaenas() = runBlocking {
        val resultado = repo.crearFaena(admin, centro().id, buzoVigente().id, enUnaHora())
        assertEquals(AppError.Forbidden, error(resultado))
    }

    @Test
    fun unSupervisorNoVeNiModificaFaenasAjenas() = runBlocking {
        val id = nuevaFaena()
        assertNull(repo.observarDetalle(otroSupervisor, id).first())
        assertTrue(repo.observarFaenas(otroSupervisor).first().isEmpty())

        val intento = repo.guardarChecklist(otroSupervisor, id, listOf(RespuestaChecklist(id, "EQ_TRAJE", EstadoEquipo.CUMPLE)))
        assertEquals(AppError.NotFound, error(intento))
        assertEquals(AppError.NotFound, error(repo.cancelar(otroSupervisor, id, "Intento no autorizado")))
    }

    @Test
    fun elAdministradorNoEditaFormulariosAunqueVeaLaFaena() = runBlocking {
        val id = nuevaFaena()
        assertNotNull(repo.observarDetalle(admin, id).first())
        val intento = repo.guardarChecklist(admin, id, listOf(RespuestaChecklist(id, "EQ_TRAJE", EstadoEquipo.CUMPLE)))
        assertEquals(AppError.Forbidden, error(intento))
    }

    @Test
    fun unSupervisorNoPuedeAutorizarSuPropiaFaena() = runBlocking {
        val id = nuevaFaena()
        completarPrechequeo(id)
        repo.enviarARevision(supervisor, id)
        assertEquals(AppError.Forbidden, error(repo.autorizar(supervisor, id)))
        assertEquals(AppError.Forbidden, error(repo.rechazar(supervisor, id, "Me arrepentí de la faena")))
        assertEquals(EstadoFaena.PENDIENTE_AUTORIZACION, estadoDe(id))
    }

    @Test
    fun noSePuedenSaltarEstados() = runBlocking {
        val id = nuevaFaena()
        // En pre-chequeo: no se autoriza ni se cierra.
        assertEquals(AppError.Forbidden, error(repo.autorizar(admin, id)))
        assertEquals(AppError.Forbidden, error(repo.registrarPosterior(supervisor, id, 30, 10.0, 100, true, true, "")))

        completarPrechequeo(id)
        repo.enviarARevision(supervisor, id)
        // Pendiente: aún no se puede registrar el posterior.
        assertEquals(AppError.Forbidden, error(repo.registrarPosterior(supervisor, id, 30, 10.0, 100, true, true, "")))
        assertEquals(EstadoFaena.PENDIENTE_AUTORIZACION, estadoDe(id))
    }

    @Test
    fun unaFaenaCerradaRechazadaOCanceladaNoCambia() = runBlocking {
        val id = nuevaFaena()
        assertTrue(repo.cancelar(supervisor, id, "Se canceló por mal tiempo") is AppResult.Ok)
        assertEquals(EstadoFaena.CANCELADA, estadoDe(id))
        assertEquals(AppError.Forbidden, error(repo.enviarARevision(supervisor, id)))
        assertEquals(AppError.Forbidden, error(repo.cancelar(supervisor, id, "Otra vez cancelando")))
    }

    @Test
    fun elRegistroPosteriorValidaLosParametros() = runBlocking {
        val id = nuevaFaena()
        completarPrechequeo(id)
        repo.enviarARevision(supervisor, id)
        repo.autorizar(admin, id)

        assertTrue(error(repo.registrarPosterior(supervisor, id, null, null, null, null, null, "")) is AppError.Validation)
        assertTrue(error(repo.registrarPosterior(supervisor, id, 0, 10.0, 100, true, true, "")) is AppError.Validation)
        assertEquals(EstadoFaena.AUTORIZADA, estadoDe(id))
        assertNull(db.registroPosteriorDao().get(id))
    }

    // ---------------------------------------------------------------- reglas de apertura

    @Test
    fun unBuzoConUnaFaenaActivaNoPuedeAbrirOtra_peroSiTrasCerrarla() = runBlocking {
        val buzo = buzoVigente()
        val primera = nuevaFaena(buzo)
        val segunda = repo.crearFaena(supervisor, centro().id, buzo.id, enUnaHora())
        assertTrue((error(segunda) as AppError.Validation).errores.containsKey("buzo"))

        repo.cancelar(supervisor, primera, "Cancelada para reprogramar")
        assertTrue(repo.crearFaena(supervisor, centro().id, buzo.id, enUnaHora()) is AppResult.Ok)
    }

    @Test
    fun noSeAsignaUnBuzoConCertificacionVencida() = runBlocking {
        val resultado = repo.crearFaena(supervisor, centro().id, buzoVencido().id, enUnaHora())
        assertTrue((error(resultado) as AppError.Validation).errores["buzo"]!!.contains("venció"))
    }

    @Test
    fun noSeAbrenFaenasConFechaPasada() = runBlocking {
        val ayer = System.currentTimeMillis() - 3L * 24 * 3_600_000
        val resultado = repo.crearFaena(supervisor, centro().id, buzoVigente().id, ayer)
        assertTrue((error(resultado) as AppError.Validation).errores.containsKey("fecha"))
    }

    @Test
    fun laAperturaExigeCentroYBuzo() = runBlocking {
        val resultado = repo.crearFaena(supervisor, null, null, enUnaHora())
        val errores = (error(resultado) as AppError.Validation).errores
        assertTrue(errores.containsKey("centro") && errores.containsKey("buzo"))
    }

    // ---------------------------------------------------------------- evidencias

    @Test
    fun lasEvidenciasSeAsocianALaFaenaYSePuedenEliminar() = runBlocking {
        val id = nuevaFaena()
        val foto = repo.agregarEvidencia(supervisor, id, TipoEvidencia.EQUIPO, "EQ_COMPRESOR", "$id/a.jpg").getOrNull()!!
        repo.agregarEvidencia(supervisor, id, TipoEvidencia.FILTROS, null, "$id/b.jpg")

        val detalle = repo.observarDetalle(supervisor, id).first()!!
        assertEquals(listOf("$id/a.jpg", "$id/b.jpg"), detalle.evidencias.map { it.ruta })
        assertEquals("EQ_COMPRESOR", detalle.evidencias.first().itemId)

        assertTrue(repo.eliminarEvidencia(supervisor, foto.id) is AppResult.Ok)
        assertEquals(listOf("$id/a.jpg"), archivos.eliminados)
        assertEquals(1, repo.observarDetalle(supervisor, id).first()!!.evidencias.size)
    }

    @Test
    fun laEvidenciaDeUnEquipoDebeReferirAUnEquipoDelCatalogo() = runBlocking {
        val id = nuevaFaena()
        assertTrue(error(repo.agregarEvidencia(supervisor, id, TipoEvidencia.EQUIPO, "EQ_INEXISTENTE", "$id/x.jpg")) is AppError.Validation)
        assertTrue(error(repo.agregarEvidencia(supervisor, id, TipoEvidencia.EQUIPO, null, "$id/x.jpg")) is AppError.Validation)
        assertTrue(error(repo.agregarEvidencia(supervisor, id, TipoEvidencia.FILTROS, "EQ_COMPRESOR", "$id/x.jpg")) is AppError.Validation)
    }

    @Test
    fun unSupervisorAjenoNoPuedeAgregarNiEliminarEvidencias() = runBlocking {
        val id = nuevaFaena()
        val foto = repo.agregarEvidencia(supervisor, id, TipoEvidencia.FILTROS, null, "$id/a.jpg").getOrNull()!!
        assertEquals(AppError.NotFound, error(repo.agregarEvidencia(otroSupervisor, id, TipoEvidencia.FILTROS, null, "$id/z.jpg")))
        assertEquals(AppError.NotFound, error(repo.eliminarEvidencia(otroSupervisor, foto.id)))
        assertTrue(archivos.eliminados.isEmpty())
    }

    // ---------------------------------------------------------------- persistencia

    @Test
    fun losDatosSobrevivenAlCerrarYReabrirLaBaseDeDatos() = runBlocking {
        val nombre = "persistencia-test.db"
        context.deleteDatabase(nombre)

        var base = Room.databaseBuilder(context, AquaFlowDatabase::class.java, nombre).allowMainThreadQueries().build()
        DatabaseSeeder(base).ensureSeeded()
        val sup = base.usuarioDao().findByRut("15892403-K")!!
        val repoA = RoomFaenaRepository(base, archivos)
        val hoy = LocalDate.now().toEpochDay()
        val buzo = base.buzoDao().observeActivos().first().first { it.certificacionVigenteHastaEpochDay > hoy }
        val centro = base.centroDao().observeActivos().first().first()
        val id = repoA.crearFaena(sup, centro.id, buzo.id, enUnaHora()).getOrNull()!!
        repoA.guardarChecklist(sup, id, listOf(RespuestaChecklist(id, "EQ_TRAJE", EstadoEquipo.OBSERVADO, "Costura floja")))
        repoA.agregarEvidencia(sup, id, TipoEvidencia.FILTROS, null, "$id/filtros.jpg")
        base.close()

        base = Room.databaseBuilder(context, AquaFlowDatabase::class.java, nombre).allowMainThreadQueries().build()
        try {
            val detalle = RoomFaenaRepository(base, archivos).observarDetalle(sup, id).first()!!
            assertEquals(buzo.nombre, detalle.faena.buzoNombre)
            assertEquals("Costura floja", detalle.checklist.single().nota)
            assertEquals("$id/filtros.jpg", detalle.evidencias.single().ruta)
            assertEquals(EstadoFaena.PRECHEQUEO, detalle.faena.estado)
        } finally {
            base.close()
            context.deleteDatabase(nombre)
        }
    }

    @Test
    fun cargarLosDatosInicialesDosVecesNoDuplicaNada() = runBlocking {
        val semillas = DatabaseSeeder(db)
        semillas.ensureSeeded()
        semillas.ensureSeeded()
        assertEquals(3, db.usuarioDao().count())
        assertEquals(3, db.centroDao().count())
        assertEquals(4, db.buzoDao().count())
    }

    // ---------------------------------------------------------------- usuarios

    @Test
    fun soloElAdministradorCreaUsuariosYElRutDebeSerUnico() = runBlocking {
        val usuarios = RoomUsuarioRepository(db.usuarioDao())
        assertEquals(
            AppError.Forbidden,
            error(usuarios.crear(supervisor, "Nuevo Usuario", "17654321-3", Rol.SUPERVISOR, "1234", "1234"))
        )

        val creado = usuarios.crear(admin, "Nuevo Usuario", "17654321-3", Rol.SUPERVISOR, "1234", "1234")
        assertTrue(creado is AppResult.Ok)
        val guardado = db.usuarioDao().findByRut("17654321-3")!!
        assertTrue("el PIN no debe guardarse en claro", guardado.pinHash != "1234" && guardado.pinHash.length > 20)

        val duplicado = usuarios.crear(admin, "Otro", "17.654.321-3", Rol.SUPERVISOR, "1234", "1234")
        assertTrue((error(duplicado) as AppError.Validation).errores.containsKey("rut"))
    }

    @Test
    fun unAdministradorNoPuedeDesactivarseASiMismo() = runBlocking {
        val usuarios = RoomUsuarioRepository(db.usuarioDao())
        assertTrue(error(usuarios.cambiarActivo(admin, admin.id, false)) is AppError.InvalidState)
        assertTrue(usuarios.cambiarActivo(admin, supervisor.id, false) is AppResult.Ok)
        // Un usuario desactivado ya no puede operar.
        assertEquals(AppError.Forbidden, error(repo.crearFaena(supervisor, centro().id, buzoVigente().id, enUnaHora())))
    }

    @Test
    fun lasFaenasDemoSeSiembranUnaSolaVezYQuedanEnEstadosTerminales() = runBlocking {
        val semillas = DatabaseSeeder(db)
        semillas.ensureSeeded(incluirFaenasDemo = true)
        semillas.ensureSeeded(incluirFaenasDemo = true)
        assertEquals(3, db.usuarioDao().count())
        val faenas = db.faenaDao().observeAll().first()
        assertEquals(3, faenas.size)
        assertTrue("las demo no deben ocupar buzos", faenas.all { it.estado.esTerminal })
        assertEquals(2, db.registroPosteriorDao().observeAll().first().size)
        // Un buzo con faena demo cerrada sigue libre para abrir una nueva.
        val id = nuevaFaena(buzoVigente(2))
        assertEquals(EstadoFaena.PRECHEQUEO, estadoDe(id))
    }
}
