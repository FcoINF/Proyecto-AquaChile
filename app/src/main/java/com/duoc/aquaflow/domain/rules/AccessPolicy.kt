package com.duoc.aquaflow.domain.rules

import com.duoc.aquaflow.model.EstadoFaena
import com.duoc.aquaflow.model.Faena
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.model.Usuario

/** Pantallas de la aplicación a efectos de control de acceso por rol. */
enum class Destino {
    HOME, HISTORIAL, PERFIL, USUARIOS, APERTURA, FAENA, DPR24, AST, SALUD, DECISION, REGISTRO
}

/**
 * Permisos de la aplicación. Se aplican en tres capas: navegación (por rol), ViewModels
 * (modo lectura/edición) y repositorios (rechazan la operación aunque la UI la permita).
 */
object AccessPolicy {

    fun puedeAccederA(rol: Rol, destino: Destino): Boolean = when (destino) {
        Destino.APERTURA -> rol == Rol.SUPERVISOR
        Destino.USUARIOS -> rol == Rol.ADMINISTRADOR
        else -> true
    }

    fun puedeGestionarUsuarios(usuario: Usuario): Boolean =
        usuario.activo && usuario.rol == Rol.ADMINISTRADOR

    fun puedeCrearFaena(usuario: Usuario): Boolean =
        usuario.activo && usuario.rol == Rol.SUPERVISOR

    /** El administrador ve todas las faenas; el supervisor solo las suyas. */
    fun puedeVer(usuario: Usuario, faena: Faena): Boolean =
        usuario.activo && (usuario.rol == Rol.ADMINISTRADOR || faena.supervisorId == usuario.id)

    private fun esSupervisorResponsable(usuario: Usuario, faena: Faena): Boolean =
        usuario.activo && usuario.rol == Rol.SUPERVISOR && faena.supervisorId == usuario.id

    /** Editar formularios, evidencias, firmar/enviar o cancelar: solo el supervisor responsable en pre-chequeo. */
    fun puedeEditarPrechequeo(usuario: Usuario, faena: Faena): Boolean =
        esSupervisorResponsable(usuario, faena) && faena.estado == EstadoFaena.PRECHEQUEO

    fun puedeDecidir(usuario: Usuario, faena: Faena): Boolean =
        usuario.activo && usuario.rol == Rol.ADMINISTRADOR &&
            faena.estado == EstadoFaena.PENDIENTE_AUTORIZACION

    fun puedeRegistrarPosterior(usuario: Usuario, faena: Faena): Boolean =
        esSupervisorResponsable(usuario, faena) && faena.estado == EstadoFaena.AUTORIZADA
}
