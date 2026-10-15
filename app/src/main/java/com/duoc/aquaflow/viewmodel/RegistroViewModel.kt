package com.duoc.aquaflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duoc.aquaflow.domain.AppResult
import com.duoc.aquaflow.domain.FieldErrors
import com.duoc.aquaflow.domain.rules.AccessPolicy
import com.duoc.aquaflow.model.EstadoFaena
import com.duoc.aquaflow.model.RegistroPosterior
import com.duoc.aquaflow.model.Usuario
import com.duoc.aquaflow.repository.FaenaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegistroForm(
    val tiempo: String = "",
    val profundidad: String = "",
    val presion: String = "",
    val sinDolorArticular: Boolean? = null,
    val respiracionNormal: Boolean? = null,
    val observaciones: String = ""
)

data class RegistroUiState(
    val cargando: Boolean = true,
    val disponible: Boolean = true,
    val codigo: String = "",
    val buzoNombre: String = "",
    val estado: EstadoFaena? = null,
    val editable: Boolean = false,
    val registro: RegistroPosterior? = null,
    val form: RegistroForm = RegistroForm(),
    val guardando: Boolean = false,
    val errores: FieldErrors = emptyMap(),
    val mensaje: String? = null
)

class RegistroViewModel(
    private val actor: Usuario,
    private val faenaId: Long,
    private val faenas: FaenaRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RegistroUiState())
    val state: StateFlow<RegistroUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            faenas.observarDetalle(actor, faenaId).collect { detalle ->
                _state.update {
                    if (detalle == null) {
                        it.copy(cargando = false, disponible = false)
                    } else {
                        it.copy(
                            cargando = false,
                            disponible = true,
                            codigo = detalle.faena.codigo,
                            buzoNombre = detalle.faena.buzoNombre,
                            estado = detalle.faena.estado,
                            editable = AccessPolicy.puedeRegistrarPosterior(actor, detalle.faena),
                            registro = detalle.registro
                        )
                    }
                }
            }
        }
    }

    private fun modificar(transformar: (RegistroForm) -> RegistroForm) = _state.update { s ->
        if (!s.editable) s else s.copy(form = transformar(s.form), errores = emptyMap())
    }

    fun onTiempo(valor: String) = modificar { it.copy(tiempo = valor.filter { c -> c.isDigit() }.take(3)) }
    fun onProfundidad(valor: String) =
        modificar { it.copy(profundidad = valor.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(5)) }
    fun onPresion(valor: String) = modificar { it.copy(presion = valor.filter { c -> c.isDigit() }.take(3)) }
    fun onSinDolor(valor: Boolean) = modificar { it.copy(sinDolorArticular = valor) }
    fun onRespiracion(valor: Boolean) = modificar { it.copy(respiracionNormal = valor) }
    fun onObservaciones(valor: String) = modificar { it.copy(observaciones = valor.take(500)) }

    fun guardar() {
        val s = _state.value
        if (!s.editable || s.guardando) return
        val errores = mutableMapOf<String, String>()
        val tiempo = s.form.tiempo.leerEntero().valorOrNullOr(errores, "tiempo")
        val profundidad = s.form.profundidad.leerDecimal().valorOrNullOr(errores, "profundidad")
        val presion = s.form.presion.leerEntero().valorOrNullOr(errores, "presion")
        if (errores.isNotEmpty()) {
            _state.update { it.copy(errores = errores) }
            return
        }
        _state.update { it.copy(guardando = true) }
        viewModelScope.launch {
            val r = faenas.registrarPosterior(
                actor, faenaId, tiempo, profundidad, presion,
                s.form.sinDolorArticular, s.form.respiracionNormal, s.form.observaciones
            )
            when (r) {
                is AppResult.Ok -> _state.update { it.copy(guardando = false, mensaje = "Faena cerrada y registro guardado.") }
                is AppResult.Fail -> _state.update {
                    it.copy(guardando = false, errores = r.error.camposConError(), mensaje = r.error.mensaje)
                }
            }
        }
    }

    fun consumirMensaje() = _state.update { it.copy(mensaje = null) }

    private fun <T> Lectura<T>.valorOrNullOr(errores: MutableMap<String, String>, campo: String): T? =
        when (this) {
            is Lectura.Valor -> valor
            Lectura.NumeroInvalido -> {
                errores[campo] = "Ingresa un número válido."
                null
            }
        }
}
