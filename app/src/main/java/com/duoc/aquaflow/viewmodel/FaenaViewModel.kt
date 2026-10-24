package com.duoc.aquaflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duoc.aquaflow.data.repository.BorradoresApertura
import com.duoc.aquaflow.data.repository.RegistroFaena
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import com.duoc.aquaflow.domain.Reglas
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

    fun iniciar(usuario: Usuario) {
        if (usuario.rol != Rol.SUPERVISOR || _ui.value.apertura.id.isNotEmpty()) return
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

    fun actualizar(apertura: PreChequeo) = _ui.update { actual ->
        if (actual.ocupado) return@update actual
        // La identidad proviene de la sesión, pero el supervisor se puede seleccionar del formulario.
        val nueva = apertura.copy(
            id = actual.apertura.id
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
                val apertura = borradores.recuperar(actual.apertura.supervisorACargo)
                _ui.value = if (apertura == null) {
                    actual.copy(mensaje = "No tienes aperturas guardadas.")
                } else {
                    FaenaUiState(apertura = apertura, mensaje = "Apertura recuperada. Revisa los datos antes de continuar.")
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

    fun registrarChecklist(checklist: ChecklistFaena): Boolean {
        if (!_ui.value.aperturaConfirmada) return false
        _ui.update { it.copy(checklist = checklist, decision = null) }
        persistir("el checklist") { r, id -> r.guardarChecklist(id, checklist) }
        return true
    }

    fun registrarEncuesta(encuesta: EncuestaSalud): Boolean {
        if (!_ui.value.aperturaConfirmada) return false
        _ui.update { it.copy(encuesta = encuesta, decision = null) }
        persistir("la encuesta de salud") { r, id -> r.guardarEncuesta(id, encuesta) }
        return true
    }

    fun registrarAst(ast: FormularioAst): Boolean {
        if (!_ui.value.aperturaConfirmada) return false
        _ui.update { it.copy(ast = ast, decision = null) }
        persistir("el AST") { r, id -> r.guardarAst(id, ast) }
        return true
    }

    fun registrarDecision(decision: RegistroDecision): Boolean {
        if (!_ui.value.aperturaConfirmada) return false
        _ui.update { it.copy(decision = decision) }
        persistir("la decisión") { r, id -> r.guardarDecision(id, decision) }
        return true
    }

    // Guarda en segundo plano. Si falla, la pantalla sigue con los datos en memoria y avisa.
    private fun persistir(que: String, accion: suspend (RegistroFaena, String) -> Unit) {
        val r = registro ?: return
        val id = _ui.value.apertura.id
        viewModelScope.launch {
            try {
                accion(r, id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val detalle = e.localizedMessage?.ifBlank { null } ?: e.javaClass.simpleName
                _ui.update { it.copy(error = "No se pudo guardar $que en el dispositivo: $detalle") }
            }
        }
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
