package com.duoc.aquaflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duoc.aquaflow.data.repository.BorradoresApertura
import com.duoc.aquaflow.data.repository.RegistroFaena
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import com.duoc.aquaflow.domain.Reglas
import com.duoc.aquaflow.model.EstadoSeccion
import com.duoc.aquaflow.model.ChecklistFaena
import com.duoc.aquaflow.model.EncuestaSalud
import com.duoc.aquaflow.model.FormularioAst
import com.duoc.aquaflow.model.PreChequeo
import com.duoc.aquaflow.model.RegistroDecision
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.model.Usuario
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class FaenaUiState(
    val apertura: PreChequeo = PreChequeo(),
    val aperturaConfirmada: Boolean = false,
    val checklist: ChecklistFaena? = null,
    val ast: FormularioAst? = null,
    val encuesta: EncuestaSalud? = null,
    val decision: RegistroDecision? = null,
    val error: String? = null,
    val ocupado: Boolean = false,
    val mensaje: String? = null,
)

// Borrador compartido por las pantallas del grafo "faena".
// `registro` guarda cada sección en Room apenas se completa; con null (pruebas) solo vive en memoria.
class FaenaViewModel(
    private val borradores: BorradoresApertura,
    private val registro: RegistroFaena? = null
) : ViewModel() {
    private val _ui = MutableStateFlow(FaenaUiState())
    val ui: StateFlow<FaenaUiState> = _ui.asStateFlow()

    fun iniciar(usuario: Usuario, aperturaId: String? = null) {
        if (usuario.rol != Rol.SUPERVISOR) return
        if (aperturaId != null) {
            cargarFaena(aperturaId)
            return
        }
        if (_ui.value.apertura.id.isNotEmpty()) return
        val ahora = Date()
        _ui.value = FaenaUiState(
            apertura = PreChequeo(
                id = UUID.randomUUID().toString(),
                fecha = SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).format(ahora),
                hora = SimpleDateFormat("HH:mm", Locale.ROOT).format(ahora),
                supervisorACargo = usuario.nombre.ifBlank { "supervisor" }
            )
        )
    }

    fun cargarFaena(id: String) {
        if (_ui.value.ocupado) return
        _ui.update { it.copy(ocupado = true, error = null, mensaje = null) }
        viewModelScope.launch {
            try {
                val guardada = borradores.recuperarFaenaPorId(id)
                _ui.value = if (guardada == null) {
                    _ui.value.copy(ocupado = false, error = "No se encontró el borrador.")
                } else {
                    FaenaUiState(
                        apertura = guardada.resumen.apertura,
                        aperturaConfirmada = Reglas.errorApertura(guardada.resumen.apertura) == null,
                        checklist = guardada.resumen.checklist,
                        ast = guardada.resumen.ast,
                        encuesta = guardada.resumen.encuesta,
                        decision = guardada.decision,
                        mensaje = "Borrador cargado para editar."
                    )
                }
            } catch (e: Exception) {
                _ui.update { it.copy(ocupado = false, error = "Error al cargar borrador: ${e.localizedMessage}") }
            } finally {
                _ui.update { it.copy(ocupado = false) }
            }
        }
    }

    fun actualizar(apertura: PreChequeo) = _ui.update { actual ->
        if (actual.ocupado || actual.decision != null) return@update actual
        // La identidad pertenece a la sesión; el formulario no puede reasignar el propietario.
        val nueva = apertura.copy(
            id = actual.apertura.id,
            supervisorACargo = actual.apertura.supervisorACargo
        )
        if (nueva == actual.apertura) actual else actual.copy(
            apertura = nueva,
            aperturaConfirmada = false,
            checklist = null,
            encuesta = null,
            ast = null,
            decision = null,
            error = null,
            mensaje = null
        )
    }

    fun confirmarApertura(): Boolean {
        val actual = completarFechaHora(_ui.value.apertura)
        val limpia = actual.copy(
            centroCultivo = actual.centroCultivo.trim(),
            fecha = actual.fecha.trim(),
            hora = actual.hora.trim(),
            buzoAsignado = actual.buzoAsignado.trim(),
            supervisorACargo = actual.supervisorACargo.trim()
        )
        val error = Reglas.errorApertura(limpia)
        _ui.update {
            it.copy(apertura = limpia, aperturaConfirmada = error == null, error = error)
        }
        return error == null
    }

    // Un borrador puede estar incompleto. Para avanzar sí se valida todo.
    fun guardarApertura(continuar: Boolean = false, alGuardar: () -> Unit = {}) {
        if (_ui.value.ocupado || _ui.value.apertura.id.isBlank()) return
        if (_ui.value.decision != null) {
            _ui.update { it.copy(error = "La faena ya fue decidida. Consulta su historial o inicia otra.") }
            return
        }
        val apertura = completarFechaHora(_ui.value.apertura)
        _ui.update { it.copy(apertura = apertura) }
        if (continuar && !confirmarApertura()) return
        _ui.update { it.copy(ocupado = true, error = null, mensaje = null) }
        viewModelScope.launch {
            try {
                borradores.guardar(_ui.value.apertura)
                _ui.update { it.copy(mensaje = "Apertura guardada en el dispositivo.") }
                alGuardar()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val detalle = e.localizedMessage?.ifBlank { null } ?: e.javaClass.simpleName
                _ui.update { it.copy(error = "No se pudo guardar la apertura: $detalle") }
            } finally {
                _ui.update { it.copy(ocupado = false) }
            }
        }
    }

    fun recuperarApertura() {
        val actual = _ui.value
        if (actual.ocupado || actual.apertura.id.isBlank()) return
        _ui.update { it.copy(ocupado = true, error = null, mensaje = null) }
        viewModelScope.launch {
            try {
                val guardada = borradores.recuperarFaena(actual.apertura.supervisorACargo)
                _ui.value = if (guardada == null) {
                    actual.copy(mensaje = "No tienes aperturas guardadas.")
                } else {
                    FaenaUiState(
                        apertura = guardada.resumen.apertura,
                        checklist = guardada.resumen.checklist, ast = guardada.resumen.ast,
                        encuesta = guardada.resumen.encuesta, decision = guardada.decision,
                        mensaje = if (guardada.decision == null) "Faena recuperada. Revisa los datos antes de continuar."
                            else "Esta faena ya fue decidida. Consúltala en Historial o inicia otra."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val detalle = e.localizedMessage?.ifBlank { null } ?: e.javaClass.simpleName
                _ui.update { it.copy(error = "No se pudo recuperar la apertura: $detalle") }
            } finally {
                _ui.update { it.copy(ocupado = false) }
            }
        }
    }

    fun registrarChecklist(checklist: ChecklistFaena, alGuardar: () -> Unit = {}): Boolean {
        if (!puedeRegistrar()) return false
        _ui.update { it.copy(checklist = checklist) }
        return persistir("el checklist", alGuardar) { r, id -> r.guardarChecklist(id, checklist) }
    }

    fun registrarEncuesta(encuesta: EncuestaSalud, alGuardar: () -> Unit = {}): Boolean {
        if (!puedeRegistrar()) return false
        _ui.update { it.copy(encuesta = encuesta) }
        return persistir("la encuesta de salud", alGuardar) { r, id -> r.guardarEncuesta(id, encuesta) }
    }

    fun registrarAst(ast: FormularioAst, alGuardar: () -> Unit = {}): Boolean {
        if (!puedeRegistrar()) return false
        _ui.update { it.copy(ast = ast) }
        return persistir("el AST", alGuardar) { r, id -> r.guardarAst(id, ast) }
    }

    fun guardarBorradorChecklist(checklist: ChecklistFaena, alGuardar: () -> Unit = {}) {
        if (!puedeGuardarBorrador()) return
        val borrador = checklist.copy(estado = EstadoSeccion.BORRADOR)
        _ui.update { it.copy(checklist = borrador) }
        persistir("el borrador del checklist", alGuardar) { r, id -> r.guardarChecklist(id, borrador) }
    }

    fun guardarBorradorEncuesta(encuesta: EncuestaSalud, alGuardar: () -> Unit = {}) {
        if (!puedeGuardarBorrador()) return
        val borrador = encuesta.copy(estado = EstadoSeccion.BORRADOR)
        _ui.update { it.copy(encuesta = borrador) }
        persistir("el borrador de salud", alGuardar) { r, id -> r.guardarEncuesta(id, borrador) }
    }

    fun guardarBorradorAst(ast: FormularioAst, alGuardar: () -> Unit = {}) {
        if (!puedeGuardarBorrador()) return
        val borrador = ast.copy(estado = EstadoSeccion.BORRADOR)
        _ui.update { it.copy(ast = borrador) }
        persistir("el borrador del AST", alGuardar) { r, id -> r.guardarAst(id, borrador) }
    }

    private fun puedeGuardarBorrador() =
        _ui.value.apertura.id.isNotBlank() && !_ui.value.ocupado && _ui.value.decision == null

    // El flujo del supervisor registra antecedentes. Las decisiones se toman desde el detalle de jefatura.
    private fun puedeRegistrar() = _ui.value.aperturaConfirmada && !_ui.value.ocupado && _ui.value.decision == null

    private fun persistir(que: String, alGuardar: () -> Unit, accion: suspend (RegistroFaena, String) -> Unit): Boolean {
        val r = registro
        if (r == null) { alGuardar(); return true }
        val id = _ui.value.apertura.id
        _ui.update { it.copy(ocupado = true, error = null) }
        viewModelScope.launch {
            try {
                accion(r, id)
                _ui.update { it.copy(ocupado = false, mensaje = "Guardado en el dispositivo.") }
                alGuardar()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update { it.copy(error = "No se pudo guardar $que. ${e.localizedMessage.orEmpty()} Intenta nuevamente.") }
            } finally {
                _ui.update { it.copy(ocupado = false) }
            }
        }
        return true
    }

    private fun completarFechaHora(apertura: PreChequeo): PreChequeo {
        if (apertura.fecha.isNotBlank() && apertura.hora.isNotBlank()) return apertura
        val ahora = Date()
        return apertura.copy(
            fecha = apertura.fecha.ifBlank { SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).format(ahora) },
            hora = apertura.hora.ifBlank { SimpleDateFormat("HH:mm", Locale.ROOT).format(ahora) }
        )
    }
}
