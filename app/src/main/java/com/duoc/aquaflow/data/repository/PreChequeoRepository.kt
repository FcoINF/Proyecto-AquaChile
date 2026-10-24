package com.duoc.aquaflow.data.repository

import com.duoc.aquaflow.data.local.AperturaEntity
import com.duoc.aquaflow.data.local.FaenaCodec
import com.duoc.aquaflow.data.local.PreChequeoDao
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
}

// Guarda cada sección de la faena a medida que el supervisor la completa.
interface RegistroFaena {
    suspend fun guardarChecklist(id: String, checklist: ChecklistFaena)
    suspend fun guardarAst(id: String, ast: FormularioAst)
    suspend fun guardarEncuesta(id: String, encuesta: EncuestaSalud)
    suspend fun guardarDecision(id: String, decision: RegistroDecision)
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

private fun AperturaEntity.mismosDatosQue(otra: AperturaEntity) =
    centroCultivo == otra.centroCultivo && fecha == otra.fecha && hora == otra.hora &&
        buzoAsignado == otra.buzoAsignado && supervisorACargo == otra.supervisorACargo

private fun PreChequeo.comoFaenaSinSecciones() =
    FaenaGuardada(ResumenFaena(apertura = this, checklist = null, ast = null, encuesta = null))

class PreChequeoRepository(private val dao: PreChequeoDao) :
    BorradoresApertura, ConsultaAperturas, RegistroFaena {

    // Si la apertura no cambió, conserva las secciones ya guardadas. Si cambió, quedan en blanco:
    // es la misma regla que aplica FaenaViewModel en pantalla al editar la apertura.
    // Se comparan solo las columnas que Room guarda (no los indicadores de la apertura en pantalla).
    override suspend fun guardar(apertura: PreChequeo) {
        require(apertura.id.isNotBlank() && apertura.supervisorACargo.isNotBlank())
        val nueva = AperturaEntity.desde(apertura, System.currentTimeMillis())
        val previa = dao.buscar(apertura.id)
        dao.guardar(
            if (previa != null && previa.mismosDatosQue(nueva)) {
                nueva.copy(
                    checklistJson = previa.checklistJson,
                    astJson = previa.astJson,
                    encuestaJson = previa.encuestaJson,
                    decisionJson = previa.decisionJson
                )
            } else nueva
        )
    }

    override suspend fun recuperar(supervisor: String): PreChequeo? =
        dao.ultimaApertura(supervisor)?.aModelo()

    override suspend fun guardarChecklist(id: String, checklist: ChecklistFaena) =
        dao.guardarChecklist(id, FaenaCodec.checklistATexto(checklist), System.currentTimeMillis())

    override suspend fun guardarAst(id: String, ast: FormularioAst) =
        dao.guardarAst(id, FaenaCodec.astATexto(ast), System.currentTimeMillis())

    override suspend fun guardarEncuesta(id: String, encuesta: EncuestaSalud) =
        dao.guardarEncuesta(id, FaenaCodec.encuestaATexto(encuesta), System.currentTimeMillis())

    override suspend fun guardarDecision(id: String, decision: RegistroDecision) {
        dao.guardarDecision(id, FaenaCodec.decisionATexto(decision), System.currentTimeMillis())
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
