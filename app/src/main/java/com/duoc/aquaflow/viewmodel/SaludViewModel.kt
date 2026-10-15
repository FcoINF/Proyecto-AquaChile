package com.duoc.aquaflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duoc.aquaflow.domain.AppResult
import com.duoc.aquaflow.domain.FieldErrors
import com.duoc.aquaflow.domain.config.ReglasConfig
import com.duoc.aquaflow.domain.rules.AccessPolicy
import com.duoc.aquaflow.domain.rules.EvaluacionSeccion
import com.duoc.aquaflow.domain.rules.FaenaEvaluator
import com.duoc.aquaflow.model.EncuestaSalud
import com.duoc.aquaflow.model.RespuestaFormulario
import com.duoc.aquaflow.model.TipoFormulario
import com.duoc.aquaflow.model.Usuario
import com.duoc.aquaflow.repository.FaenaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SaludForm(
    val sistolica: String = "",
    val diastolica: String = "",
    val saturacion: String = "",
    val temperatura: String = "",
    val observaciones: String = "",
    /** Solo las respondidas. true = respuesta favorable (apto). */
    val declaraciones: Map<String, Boolean> = emptyMap()
)

data class SaludUiState(
    val cargando: Boolean = true,
    val disponible: Boolean = true,
    val codigo: String = "",
    val buzoNombre: String = "",
    val editable: Boolean = false,
    val form: SaludForm = SaludForm(),
    val evaluacion: EvaluacionSeccion = EvaluacionSeccion(),
    val sinGuardar: Boolean = false,
    val guardando: Boolean = false,
    val errores: FieldErrors = emptyMap(),
    val mensaje: String? = null
)

class SaludViewModel(
    private val actor: Usuario,
    private val faenaId: Long,
    private val faenas: FaenaRepository,
    private val reglas: ReglasConfig
) : ViewModel() {

    private val _state = MutableStateFlow(SaludUiState())
    val state: StateFlow<SaludUiState> = _state.asStateFlow()

    private var inicializado = false
    private var guardado = SaludForm()

    init {
        viewModelScope.launch {
            faenas.observarDetalle(actor, faenaId).collect { detalle ->
                if (detalle == null) {
                    _state.update { it.copy(cargando = false, disponible = false) }
                } else {
                    if (!inicializado) {
                        inicializado = true
                        val e = detalle.encuestaSalud
                        guardado = SaludForm(
                            sistolica = e?.sistolica?.toString().orEmpty(),
                            diastolica = e?.diastolica?.toString().orEmpty(),
                            saturacion = e?.saturacionO2?.toString().orEmpty(),
                            temperatura = e?.temperaturaC?.let { formatearDecimal(it) }.orEmpty(),
                            observaciones = e?.observaciones.orEmpty(),
                            declaraciones = detalle.respuestasDe(TipoFormulario.SALUD_DECLARACION)
                        )
                        _state.update { it.copy(form = guardado, evaluacion = evaluar(guardado)) }
                    }
                    _state.update {
                        it.copy(
                            cargando = false,
                            disponible = true,
                            codigo = detalle.faena.codigo,
                            buzoNombre = detalle.faena.buzoNombre,
                            editable = AccessPolicy.puedeEditarPrechequeo(actor, detalle.faena)
                        )
                    }
                }
            }
        }
    }

    private fun modificar(transformar: (SaludForm) -> SaludForm) = _state.update { s ->
        if (!s.editable) {
            s
        } else {
            val nuevo = transformar(s.form)
            s.copy(form = nuevo, sinGuardar = nuevo != guardado, evaluacion = evaluar(nuevo), errores = emptyMap())
        }
    }

    private fun soloDigitos(valor: String, max: Int) = valor.filter { it.isDigit() }.take(max)

    fun onSistolica(valor: String) = modificar { it.copy(sistolica = soloDigitos(valor, 3)) }
    fun onDiastolica(valor: String) = modificar { it.copy(diastolica = soloDigitos(valor, 3)) }
    fun onSaturacion(valor: String) = modificar { it.copy(saturacion = soloDigitos(valor, 3)) }
    fun onTemperatura(valor: String) =
        modificar { it.copy(temperatura = valor.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(5)) }
    fun onObservaciones(valor: String) = modificar { it.copy(observaciones = valor.take(500)) }
    fun onDeclaracion(preguntaId: String, favorable: Boolean) =
        modificar { it.copy(declaraciones = it.declaraciones + (preguntaId to favorable)) }

    fun guardar() {
        val s = _state.value
        if (!s.editable || s.guardando) return
        val errores = mutableMapOf<String, String>()
        val sistolica = entero(s.form.sistolica, "sistolica", errores)
        val diastolica = entero(s.form.diastolica, "diastolica", errores)
        val saturacion = entero(s.form.saturacion, "spo2", errores)
        val temperatura = when (val l = s.form.temperatura.leerDecimal()) {
            is Lectura.Valor -> l.valor
            Lectura.NumeroInvalido -> {
                errores["temperatura"] = "Ingresa un número válido."
                null
            }
        }
        if (errores.isNotEmpty()) {
            _state.update { it.copy(errores = errores) }
            return
        }
        val encuesta = EncuestaSalud(
            faenaId = faenaId,
            sistolica = sistolica,
            diastolica = diastolica,
            saturacionO2 = saturacion,
            temperaturaC = temperatura,
            observaciones = s.form.observaciones,
            actualizadoAt = 0L
        )
        _state.update { it.copy(guardando = true) }
        viewModelScope.launch {
            when (val r = faenas.guardarSalud(actor, faenaId, encuesta, s.form.declaraciones)) {
                is AppResult.Ok -> {
                    guardado = s.form
                    _state.update { it.copy(guardando = false, sinGuardar = it.form != guardado, mensaje = "Encuesta de salud guardada.") }
                }
                is AppResult.Fail -> _state.update {
                    it.copy(guardando = false, errores = r.error.camposConError(), mensaje = r.error.mensaje)
                }
            }
        }
    }

    fun consumirMensaje() = _state.update { it.copy(mensaje = null) }

    private fun entero(texto: String, campo: String, errores: MutableMap<String, String>): Int? =
        when (val l = texto.leerEntero()) {
            is Lectura.Valor -> l.valor
            Lectura.NumeroInvalido -> {
                errores[campo] = "Ingresa un número válido."
                null
            }
        }

    private fun evaluar(form: SaludForm): EvaluacionSeccion {
        val encuesta = EncuestaSalud(
            faenaId = faenaId,
            sistolica = form.sistolica.toIntOrNull(),
            diastolica = form.diastolica.toIntOrNull(),
            saturacionO2 = form.saturacion.toIntOrNull(),
            temperaturaC = form.temperatura.replace(',', '.').toDoubleOrNull(),
            observaciones = form.observaciones,
            actualizadoAt = 0L
        )
        val respuestas = form.declaraciones.map { (k, v) ->
            RespuestaFormulario(faenaId, TipoFormulario.SALUD_DECLARACION, k, v)
        }
        return FaenaEvaluator.evaluarSalud(encuesta, respuestas, reglas)
    }
}
