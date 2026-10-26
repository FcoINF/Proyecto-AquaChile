package com.duoc.aquaflow.viewmodel

import com.duoc.aquaflow.model.AntecedentesAst
import com.duoc.aquaflow.model.EstadoSeccion
import androidx.lifecycle.ViewModel
import com.duoc.aquaflow.domain.ReglasAst
import com.duoc.aquaflow.model.FormularioAst
import com.duoc.aquaflow.model.PREGUNTAS_AST
import com.duoc.aquaflow.model.PasoAst
import com.duoc.aquaflow.model.ResultadoAst
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// Todo lo que muestra el formulario AST en un solo objeto inmutable.
data class AstUiState(
    val tipologia: String = "",
    val antecedentes: AntecedentesAst = AntecedentesAst(),
    val requiereNuevoAst: Boolean = false,
    val respuestas: List<Boolean?> = List(PREGUNTAS_AST.size) { null },   // null = sin responder
    val pasos: List<PasoAst> = emptyList(),
    val error: String? = null
) {
    // Semáforo en vivo: aparece cuando ya se respondió todo el chequeo inicial
    val resultado: ResultadoAst?
        get() = if (ReglasAst.respuestasCompletas(respuestas))
            ReglasAst.resultado(respuestas, pasos, requiereNuevoAst) else null
}

class AstViewModel : ViewModel() {
    private val _ui = MutableStateFlow(AstUiState())
    val ui: StateFlow<AstUiState> = _ui.asStateFlow()

    private var astCargado = false

    // Restaura un AST ya registrado (por ejemplo al volver atrás desde la encuesta de salud).
    // Solo la primera vez: así no pisa lo que el usuario edite después.
    fun cargar(f: FormularioAst) {
        if (astCargado) return
        astCargado = true
        _ui.value = AstUiState(
            tipologia = f.tipologia,
            requiereNuevoAst = f.requiereNuevoAst,
            respuestas = if (f.versionPreguntas >= 2) f.respuestas else List(PREGUNTAS_AST.size) { null },
            antecedentes = f.antecedentes,
            pasos = f.pasos
        )
    }

    fun antecedentes(v: AntecedentesAst) = _ui.update { it.copy(antecedentes = v, error = null) }

    fun elegirTipologia(v: String) = _ui.update { it.copy(tipologia = v, error = null) }
    fun alternarNuevoAst() = _ui.update { it.copy(requiereNuevoAst = !it.requiereNuevoAst, error = null) }

    fun responder(indice: Int, valor: Boolean) = _ui.update {
        if (indice !in it.respuestas.indices) it
        else it.copy(respuestas = it.respuestas.toMutableList().also { l -> l[indice] = valor }, error = null)
    }

    fun cambiarPaso(indice: Int, paso: PasoAst) = _ui.update {
        if (indice !in it.pasos.indices) it
        else it.copy(pasos = it.pasos.toMutableList().also { l -> l[indice] = paso }, error = null)
    }

    fun agregarPaso() = _ui.update {
        if (it.pasos.size >= ReglasAst.MAX_PASOS) it else it.copy(pasos = it.pasos + PasoAst(), error = null)
    }

    // Siempre queda al menos un paso
    fun quitarPaso(indice: Int) = _ui.update {
        if (it.pasos.size <= 1 || indice !in it.pasos.indices) it
        else it.copy(pasos = it.pasos.filterIndexed { i, _ -> i != indice }, error = null)
    }

    // Devuelve el formulario si es válido; si no, deja el mensaje en `ui.error` y devuelve null.
    fun guardar(): FormularioAst? {
        val s = _ui.value
        val error = ReglasAst.error(s.tipologia, s.respuestas, s.pasos)
        if (error != null) {
            _ui.update { it.copy(error = error) }
            return null
        }
        return FormularioAst(
            tipologia = s.tipologia,
            antecedentes = s.antecedentes,
            requiereNuevoAst = s.requiereNuevoAst,
            respuestas = s.respuestas.map { it == true },
            pasos = s.pasos.map { it.copy(tarea = it.tarea.trim(), peligro = it.peligro.trim(), control = it.control.trim()) },
            resultado = ReglasAst.resultado(s.respuestas, s.pasos, s.requiereNuevoAst)
        )
    }

    fun borrador(): FormularioAst {
        val s = _ui.value
        return FormularioAst(
            tipologia = s.tipologia,
            antecedentes = s.antecedentes,
            requiereNuevoAst = s.requiereNuevoAst,
            respuestas = s.respuestas.map { it == true },
            pasos = s.pasos.map { it.copy(tarea = it.tarea.trim(), peligro = it.peligro.trim(), control = it.control.trim()) },
            resultado = s.resultado ?: ResultadoAst.OBSERVADO,
            estado = EstadoSeccion.BORRADOR
        )
    }
}