package com.duoc.aquaflow

import com.duoc.aquaflow.data.local.dao.UsuarioDao
import com.duoc.aquaflow.domain.AppResult
import com.duoc.aquaflow.model.AntecedentesAst
import com.duoc.aquaflow.model.EncuestaSalud
import com.duoc.aquaflow.model.Evidencia
import com.duoc.aquaflow.model.Faena
import com.duoc.aquaflow.model.FaenaDetalle
import com.duoc.aquaflow.model.RegistroPosterior
import com.duoc.aquaflow.model.RespuestaChecklist
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.model.SessionState
import com.duoc.aquaflow.model.TipoEvidencia
import com.duoc.aquaflow.model.Usuario
import com.duoc.aquaflow.repository.AuthRepository
import com.duoc.aquaflow.repository.EvidenciaArchivos
import com.duoc.aquaflow.repository.FaenaRepository
import com.duoc.aquaflow.repository.SessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** Reemplaza el dispatcher Main para poder probar ViewModels en la JVM. */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(private val dispatcher: TestDispatcher = UnconfinedTestDispatcher()) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

class FakeUsuarioDao(inicial: List<Usuario> = emptyList()) : UsuarioDao {
    val datos = inicial.toMutableList()

    override suspend fun insert(usuario: Usuario): Long {
        val id = (datos.maxOfOrNull { it.id } ?: 0L) + 1
        datos += usuario.copy(id = id)
        return id
    }

    override suspend fun insertAll(usuarios: List<Usuario>) {
        usuarios.forEach { insert(it) }
    }

    override suspend fun update(usuario: Usuario) {
        val indice = datos.indexOfFirst { it.id == usuario.id }
        if (indice >= 0) datos[indice] = usuario
    }

    override suspend fun findById(id: Long): Usuario? = datos.firstOrNull { it.id == id }

    override suspend fun findByRut(rut: String): Usuario? = datos.firstOrNull { it.rut == rut }

    override fun observeAll(): Flow<List<Usuario>> = flowOf(datos.toList())

    override suspend fun count(): Int = datos.size
}

class FakeSessionStore(var usuarioId: Long? = null) : SessionStore {
    override fun leerUsuarioId(): Long? = usuarioId
    override fun guardar(usuarioId: Long) {
        this.usuarioId = usuarioId
    }

    override fun limpiar() {
        usuarioId = null
    }
}

class FakeAuthRepository : AuthRepository {
    override val session = MutableStateFlow<SessionState>(SessionState.LoggedOut)
    var resultadoLogin: AppResult<Usuario> = AppResult.Ok(TestData.usuario())
    var pinCorrecto = true
    val llamadasLogin = mutableListOf<Triple<String, String, Rol>>()

    override suspend fun restoreSession() = Unit

    override suspend fun login(rut: String, pin: String, rol: Rol): AppResult<Usuario> {
        llamadasLogin += Triple(rut, pin, rol)
        return resultadoLogin
    }

    override suspend fun logout() {
        session.value = SessionState.LoggedOut
    }

    override suspend fun verifyPin(usuarioId: Long, pin: String): Boolean = pinCorrecto
}

class FakeEvidenciaArchivos : EvidenciaArchivos {
    val eliminados = mutableListOf<String>()
    override fun eliminar(rutaRelativa: String) {
        eliminados += rutaRelativa
    }
}

/** Repositorio de faenas controlable desde los tests: expone un detalle y registra las llamadas. */
class FakeFaenaRepository(detalle: FaenaDetalle?) : FaenaRepository {
    val detalleFlow = MutableStateFlow(detalle)

    var resultadoEnviar: AppResult<Unit> = AppResult.Ok(Unit)
    var resultadoAutorizar: AppResult<Unit> = AppResult.Ok(Unit)
    var resultadoRechazar: AppResult<Unit> = AppResult.Ok(Unit)
    var resultadoChecklist: AppResult<Unit> = AppResult.Ok(Unit)
    var resultadoEvidencia: AppResult<Evidencia> = AppResult.Ok(
        Evidencia(id = 1, faenaId = 10, tipo = TipoEvidencia.FILTROS, ruta = "10/a.jpg", creadoAt = 0)
    )

    var llamadasEnviar = 0
    var llamadasAutorizar = 0
    var llamadasRechazar = 0
    var ultimoMotivo: String? = null
    var ultimoChecklist: List<RespuestaChecklist>? = null
    var registros: Map<Long, RegistroPosterior> = emptyMap()

    override fun observarFaenas(actor: Usuario): Flow<List<Faena>> = flowOf(emptyList())

    override fun observarDetalle(actor: Usuario, faenaId: Long): Flow<FaenaDetalle?> = detalleFlow

    override fun observarPendientesSync(): Flow<Int> = flowOf(0)

    override fun observarRegistros(): Flow<Map<Long, RegistroPosterior>> = flowOf(registros)

    override suspend fun crearFaena(actor: Usuario, centroId: Long?, buzoId: Long?, fechaInicio: Long): AppResult<Long> =
        AppResult.Ok(1L)

    override suspend fun guardarChecklist(actor: Usuario, faenaId: Long, respuestas: List<RespuestaChecklist>): AppResult<Unit> {
        ultimoChecklist = respuestas
        return resultadoChecklist
    }

    override suspend fun guardarAst(
        actor: Usuario,
        faenaId: Long,
        antecedentes: AntecedentesAst,
        gating: Map<String, Boolean>,
        riesgos: Map<String, Boolean>
    ): AppResult<Unit> = AppResult.Ok(Unit)

    override suspend fun guardarSalud(
        actor: Usuario,
        faenaId: Long,
        encuesta: EncuestaSalud,
        declaraciones: Map<String, Boolean>
    ): AppResult<Unit> = AppResult.Ok(Unit)

    override suspend fun agregarEvidencia(
        actor: Usuario,
        faenaId: Long,
        tipo: TipoEvidencia,
        itemId: String?,
        rutaRelativa: String
    ): AppResult<Evidencia> = resultadoEvidencia

    override suspend fun eliminarEvidencia(actor: Usuario, evidenciaId: Long): AppResult<Unit> = AppResult.Ok(Unit)

    override suspend fun enviarARevision(actor: Usuario, faenaId: Long): AppResult<Unit> {
        llamadasEnviar++
        return resultadoEnviar
    }

    override suspend fun autorizar(actor: Usuario, faenaId: Long): AppResult<Unit> {
        llamadasAutorizar++
        return resultadoAutorizar
    }

    override suspend fun rechazar(actor: Usuario, faenaId: Long, motivo: String): AppResult<Unit> {
        llamadasRechazar++
        ultimoMotivo = motivo
        return resultadoRechazar
    }

    override suspend fun cancelar(actor: Usuario, faenaId: Long, motivo: String): AppResult<Unit> = AppResult.Ok(Unit)

    override suspend fun registrarPosterior(
        actor: Usuario,
        faenaId: Long,
        tiempoInmersionMin: Int?,
        profundidadMaxM: Double?,
        presionBancoBar: Int?,
        sinDolorArticular: Boolean?,
        respiracionNormal: Boolean?,
        observaciones: String
    ): AppResult<Unit> = AppResult.Ok(Unit)
}
