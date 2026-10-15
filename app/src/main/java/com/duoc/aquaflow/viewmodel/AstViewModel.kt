package com.duoc.aquaflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duoc.aquaflow.domain.AppResult
import com.duoc.aquaflow.domain.FieldErrors
import com.duoc.aquaflow.domain.config.ReglasConfig
import com.duoc.aquaflow.domain.rules.AccessPolicy
import com.duoc.aquaflow.domain.rules.EvaluacionSeccion
import com.duoc.aquaflow.domain.rules.FaenaEvaluator
import com.duoc.aquaflow.model.AntecedentesAst
import com.duoc.aquaflow.model.RespuestaFormulario
import com.duoc.aquaflow.model.TipoFormulario
import com.duoc.aquaflow.model.Usuario
import com.duoc.aquaflow.repository.FaenaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AstForm(
    val tipologia: String = "",
    val tarea: String = "",
    val pasos: String = "",
    val profundidad: String = "",
    val requiereNuevoAst: Boolean = false,
    /** Solo las preguntas respondidas. true = "Sí (apto)". */
    val gating: Map<String, Boolean> = emptyMap(),
    /** Riesgos con su medida de control verificada. */
    val riesgos: Map<String, Boolean> = emptyMap()
)

data class AstUiState(
    val cargando: Boolean = true,
    val disponible: Boolean = true,
    val codigo: String = "",
    val centro: String = "",
    val fechaInicio: Long = 0L,
    val editable: Boolean = false,
    val form: AstForm = AstForm(),
    val evaluacion: EvaluacionSeccion = EvaluacionSeccion(),
    val sinGuardar: Boolean = false,
    val guardando: Boolean = false,
    val errores: FieldErrors = emptyMap(),
    val mensaje: String? = null
)

class AstViewModel(
    private val actor: Usuario,
    private val faenaId: Long,
    private val faenas: FaenaRepository,
    private val reglas: ReglasConfig
) : ViewModel() {

    private val _state = MutableStateFlow(AstUiState())
    val state: StateFlow<AstUiState> = _state.asStateFlow()

    private var inicializado = false
    private var guardado = AstForm()

    init {
        viewModelScope.launch {
            faenas.observarDetalle(actor, faenaId).collect { detalle ->
                if (detalle == null) {
                    _state.update { it.copy(cargando = false, disponible = false) }
                } else {
                    if (!inicializado) {
                        inicializado = true
                        val ast = detalle.antecedentesAst
                        guardado = AstForm(
                            tipologia = ast?.tipologia.orEmpty(),
                            tarea = ast?.tarea.orEmpty(),
                            pasos = ast?.pasos.orEmpty(),
                            profundidad = ast?.profundidadMaxPlanificadaM?.let { formatearDecimal(it) }.orEmpty(),
                            requiereNuevoAst = ast?.requiereNuevoAst ?: false,
                            gating = detalle.respuestasDe(TipoFormulario.AST_GATING),
                            riesgos = detalle.respuestasDe(TipoFormulario.AST_RIESGO)
                        )
                        _state.update { it.copy(form = guardado, evaluacion = evaluar(guardado)) }
                    }
                    _state.update {
                        it.copy(
                            cargando = false,
                            disponible = true,
                            codigo = detalle.faena.codigo,
                            centro = detalle.faena.centroNombre,
                            fechaInicio = detalle.faena.fechaInicio,
                            editable = AccessPolicy.puedeEditarPrechequeo(actor, detalle.faena)
                        )
                    }
                }
            }
        }
    }

    private fun modificar(transformar: (AstForm) -> AstForm) = _state.update { s ->
        if (!s.editable) {
            s
        } else {
            val nuevo = transformar(s.form)
            s.copy(form = nuevo, sinGuardar = nuevo != guardado, evaluacion = evaluar(nuevo), errores = emptyMap())
        }
    }

    fun onTipologia(valor: String) = modificar { it.copy(tipologia = valor.take(80)) }
    fun onTarea(valor: String) = modificar { it.copy(tarea = valor.take(300)) }
    fun onPasos(valor: String) = modificar { it.copy(pasos = valor.take(2000)) }
    fun onProfundidad(valor: String) = modificar { it.copy(profundidad = valor.take(6)) }
    fun onRequiereNuevoAst(valor: Boolean) = modificar { it.copy(requiereNuevoAst = valor) }
    fun onGating(preguntaId: String, apto: Boolean) = modificar { it.copy(gating = it.gating + (preguntaId to apto)) }
    fun onRiesgo(riesgoId: String, verificado: Boolean) = modificar { it.copy(riesgos = it.riesgos + (riesgoId to verificado)) }

    fun guardar() {
        val s = _state.value
        if (!s.editable || s.guardando) return
        val profundidad = when (val lectura = s.form.profundidad.leerDecimal()) {
            is Lectura.Valor -> lectura.valor
            Lectura.NumeroInvalido -> {
                _state.update { it.copy(errores = mapOf("profundidad" to "Ingresa un número válido.")) }
                return
            }
        }
        val antecedentes = AntecedentesAst(
            faenaId = faenaId,
            tipologia = s.form.tipologia,
            tarea = s.form.tarea,
            pasos = s.form.pasos,
            profundidadMaxPlanificadaM = profundidad,
            requiereNuevoAst = s.form.requiereNuevoAst,
            actualizadoAt = 0L
        )
        _state.update { it.copy(guardando = true) }
        viewModelScope.launch {
            when (val r = faenas.guardarAst(actor, faenaId, antecedentes, s.form.gating, s.form.riesgos)) {
                is AppResult.Ok -> {
                    guardado = s.form
                    _state.update { it.copy(guardando = false, sinGuardar = it.form != guardado, mensaje = "Formulario AST guardado.") }
                }
                is AppResult.Fail -> _state.update {
                    it.copy(guardando = false, errores = r.error.camposConError(), mensaje = r.error.mensaje)
                }
            }
        }
    }

    fun consumirMensaje() = _state.update { it.copy(mensaje = null) }

    private fun evaluar(form: AstForm): EvaluacionSeccion {
        val antecedentes = AntecedentesAst(
            faenaId = faenaId,
            tipologia = form.tipologia,
            tarea = form.tarea,
            pasos = form.pasos,
            profundidadMaxPlanificadaM = form.profundidad.leerDecimal().valorOrNull(),
            requiereNuevoAst = form.requiereNuevoAst,
            actualizadoAt = 0L
        )
        val respuestas = form.gating.map { (k, v) -> RespuestaFormulario(faenaId, TipoFormulario.AST_GATING, k, v) } +
            form.riesgos.map { (k, v) -> RespuestaFormulario(faenaId, TipoFormulario.AST_RIESGO, k, v) }
        return FaenaEvaluator.evaluarAst(antecedentes, respuestas, reglas)
    }
}

/** Texto de un decimal sin ceros sobrantes (30.0 -> "30", 12.5 -> "12,5"). */
internal fun formatearDecimal(valor: Double): String {
    val texto = if (valor == valor.toLong().toDouble()) valor.toLong().toString() else valor.toString()
    return texto.replace('.', ',')
}
