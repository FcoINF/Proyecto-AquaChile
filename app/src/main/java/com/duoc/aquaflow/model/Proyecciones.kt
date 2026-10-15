package com.duoc.aquaflow.model

/** Estado de la sesión de la aplicación. */
sealed interface SessionState {
    data object Loading : SessionState
    data object LoggedOut : SessionState
    data class LoggedIn(val usuario: Usuario) : SessionState
}

/** Vista consolidada de una faena con todos sus formularios y evidencias. */
data class FaenaDetalle(
    val faena: Faena,
    val checklist: List<RespuestaChecklist>,
    val antecedentesAst: AntecedentesAst?,
    val respuestas: List<RespuestaFormulario>,
    val encuestaSalud: EncuestaSalud?,
    val evidencias: List<Evidencia>,
    val registro: RegistroPosterior?,
    val eventos: List<EventoFaena>
) {
    fun respuestasDe(tipo: TipoFormulario): Map<String, Boolean> =
        respuestas.filter { it.formulario == tipo }.associate { it.clave to it.respuesta }
}
