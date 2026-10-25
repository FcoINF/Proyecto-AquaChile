package com.duoc.aquaflow.data.repository

import com.duoc.aquaflow.data.local.AperturaEntity
import com.duoc.aquaflow.data.local.FaenaCodec
import com.duoc.aquaflow.data.local.PreChequeoDao
import com.duoc.aquaflow.domain.ReglasCierre
import com.duoc.aquaflow.model.RegistroCierre
import com.duoc.aquaflow.model.ChecklistFaena
import com.duoc.aquaflow.model.EncuestaSalud
import com.duoc.aquaflow.model.FaenaGuardada
import com.duoc.aquaflow.model.FormularioAst
import com.duoc.aquaflow.model.PreChequeo
import com.duoc.aquaflow.model.RegistroDecision
import com.duoc.aquaflow.model.ResumenFaena
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.model.Usuario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface BorradoresApertura {
    suspend fun guardar(apertura: PreChequeo)
    suspend fun recuperar(supervisor: String): PreChequeo?
    suspend fun recuperarFaena(supervisor: String): FaenaGuardada? =
        recuperar(supervisor)?.let { FaenaGuardada(ResumenFaena(it, null, null, null)) }
}

// Guarda cada sección de la faena a medida que el supervisor la completa.
interface RegistroFaena {
    suspend fun guardarChecklist(id: String, checklist: ChecklistFaena)
    suspend fun guardarAst(id: String, ast: FormularioAst)
    suspend fun guardarEncuesta(id: String, encuesta: EncuestaSalud)
    suspend fun guardarDecision(id: String, decision: RegistroDecision)
    suspend fun decidir(id: String, decision: RegistroDecision, usuario: Usuario) {
        require(usuario.rol == Rol.JEFE) { "Solo la jefatura puede decidir." }
        guardarDecision(id, decision.copy(decididoPor = usuario.nombre))
    }
}

interface CierreFaenas {
    suspend fun cerrar(id: String, cierre: RegistroCierre, usuario: Usuario)
}

interface ConsultaAperturas {
    fun observar(usuario: Usuario): Flow<List<PreChequeo>>
    fun observarDetalle(id: String, usuario: Usuario): Flow<PreChequeo?>

    // Por defecto solo hay aperturas (sin secciones ni decisión). El repositorio real lo reemplaza.
    fun observarFaenas(usuario: Usuario): Flow<List<FaenaGuardada>> =
        observar(usuario).map { lista -> lista.map { it.comoFaenaSinSecciones() } }

    fun observarFaena(id: String, usuario: Usuario): Flow<FaenaGuardada?> =
        observarDetalle(id, usuario).map { it?.comoFaenaSinSecciones() }
}

private fun PreChequeo.comoFaenaSinSecciones() =
    FaenaGuardada(ResumenFaena(apertura = this, checklist = null, ast = null, encuesta = null))

class PreChequeoRepository(private val dao: PreChequeoDao) :
    BorradoresApertura, ConsultaAperturas, RegistroFaena, CierreFaenas {

    // Si la apertura no cambió, conserva las secciones ya guardadas. Si cambió, quedan en blanco:
    // es la misma regla que aplica FaenaViewModel en pantalla al editar la apertura.
    // La comparación incluye las condiciones iniciales persistidas.
    override suspend fun guardar(apertura: PreChequeo) {
        require(apertura.id.isNotBlank() && apertura.supervisorACargo.isNotBlank())
        val nueva = AperturaEntity.desde(apertura, System.currentTimeMillis())
        dao.guardarBorrador(nueva)
    }

    override suspend fun recuperar(supervisor: String): PreChequeo? =
        dao.ultimaApertura(supervisor)?.aModelo()

    override suspend fun recuperarFaena(supervisor: String): FaenaGuardada? =
        dao.ultimaApertura(supervisor)?.aFaena()

    override suspend fun guardarChecklist(id: String, checklist: ChecklistFaena) {
        require(id == checklist.idFaena) { "El checklist no corresponde a la faena." }
        check(dao.guardarChecklist(id, FaenaCodec.checklistATexto(checklist), System.currentTimeMillis()) == 1) {
            "La faena no existe o ya fue decidida."
        }
    }
    override suspend fun guardarAst(id: String, ast: FormularioAst) {
        check(dao.guardarAst(id, FaenaCodec.astATexto(ast), System.currentTimeMillis()) == 1) {
            "La faena no existe o ya fue decidida."
        }
    }
    override suspend fun guardarEncuesta(id: String, encuesta: EncuestaSalud) {
        check(dao.guardarEncuesta(id, FaenaCodec.encuestaATexto(encuesta), System.currentTimeMillis()) == 1) {
            "La faena no existe o ya fue decidida."
        }
    }
    override suspend fun decidir(id: String, decision: RegistroDecision, usuario: Usuario) {
        require(usuario.rol == Rol.JEFE) { "Solo la jefatura puede decidir." }
        guardarDecision(id, decision.copy(decididoPor = usuario.nombre, motivo = decision.motivo.trim()))
    }

    override suspend fun guardarDecision(id: String, decision: RegistroDecision) {
        dao.guardarDecisionValidada(id, decision)
    }

    override suspend fun cerrar(id: String, cierre: RegistroCierre, usuario: Usuario) {
        val fila = checkNotNull(dao.buscar(id)) { "La faena no existe." }
        check(ReglasCierre.puedeCerrar(fila.aFaena(), usuario)) { "Solo se puede cerrar una faena autorizada propia o desde jefatura, una vez." }
        val error = ReglasCierre.errorCampos(cierre.duracionMinutos, cierre.observaciones, cierre.revisionSalida)
        require(error == null) { error.orEmpty() }
        val ahora = System.currentTimeMillis()
        val registro = cierre.copy(observaciones = cierre.observaciones.trim(), cerradoPor = usuario.nombre, cerradoEn = ahora)
        check(dao.guardarCierre(id, FaenaCodec.cierreATexto(registro), ahora, requireNotNull(fila.decisionJson)) == 1) {
            "La faena cambió o ya tiene un cierre. Actualiza el detalle."
        }
    }

    override fun observar(usuario: Usuario): Flow<List<PreChequeo>> =
        dao.observarAperturas(supervisorVisible(usuario)).map { lista -> lista.map { it.aModelo() } }

    override fun observarDetalle(id: String, usuario: Usuario): Flow<PreChequeo?> =
        dao.observarApertura(id, supervisorVisible(usuario)).map { it?.aModelo() }

    override fun observarFaenas(usuario: Usuario): Flow<List<FaenaGuardada>> =
        dao.observarAperturas(supervisorVisible(usuario)).map { lista -> lista.map { it.aFaena() } }

    override fun observarFaena(id: String, usuario: Usuario): Flow<FaenaGuardada?> =
        dao.observarApertura(id, supervisorVisible(usuario)).map { it?.aFaena() }

    private fun supervisorVisible(usuario: Usuario): String? = when (usuario.rol) {
        Rol.JEFE -> null
        Rol.SUPERVISOR -> usuario.nombre
    }
}
