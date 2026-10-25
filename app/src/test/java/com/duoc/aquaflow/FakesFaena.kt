package com.duoc.aquaflow

import com.duoc.aquaflow.data.local.AperturaEntity
import com.duoc.aquaflow.data.local.PreChequeoDao
import com.duoc.aquaflow.data.repository.RegistroFaena
import com.duoc.aquaflow.model.ChecklistFaena
import com.duoc.aquaflow.model.EncuestaSalud
import com.duoc.aquaflow.model.FormularioAst
import com.duoc.aquaflow.model.RegistroDecision
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

// Imita las consultas de Room en memoria, para probar el repositorio sin Android.
class DaoMemoria : PreChequeoDao {
    val filas = MutableStateFlow<Map<String, AperturaEntity>>(emptyMap())

    override suspend fun guardar(apertura: AperturaEntity) = filas.update { it + (apertura.id to apertura) }
    override suspend fun buscar(id: String) = filas.value[id]
    override suspend fun ultimaApertura(supervisor: String) =
        filas.value.values.filter { it.supervisorACargo == supervisor }.maxByOrNull { it.actualizadoEn }

    override fun observarAperturas(supervisor: String?): Flow<List<AperturaEntity>> = filas.map { m ->
        m.values.filter { supervisor == null || it.supervisorACargo == supervisor }
            .sortedByDescending { it.actualizadoEn }
    }

    override fun observarApertura(id: String, supervisor: String?): Flow<AperturaEntity?> = filas.map { m ->
        m[id]?.takeIf { supervisor == null || it.supervisorACargo == supervisor }
    }

    private fun cambiar(id: String, f: (AperturaEntity) -> AperturaEntity) =
        filas.update { m -> m[id]?.let { m + (id to f(it)) } ?: m }

    private fun cambiarEditable(id: String, f: (AperturaEntity) -> AperturaEntity): Int {
        val actual = filas.value[id] ?: return 0
        if (actual.decisionJson != null) return 0
        cambiar(id, f)
        return 1
    }
    override suspend fun guardarChecklist(id: String, json: String, ahora: Long) =
        cambiarEditable(id) { it.copy(checklistJson = json, actualizadoEn = ahora) }
    override suspend fun guardarAst(id: String, json: String, ahora: Long) =
        cambiarEditable(id) { it.copy(astJson = json, actualizadoEn = ahora) }
    override suspend fun guardarEncuesta(id: String, json: String, ahora: Long) =
        cambiarEditable(id) { it.copy(encuestaJson = json, actualizadoEn = ahora) }

    override suspend fun guardarCierre(id: String, json: String, ahora: Long, decisionEsperada: String): Int {
        val actual = filas.value[id] ?: return 0
        if (actual.cierreJson != null || actual.decisionJson != decisionEsperada) return 0
        cambiar(id) { it.copy(cierreJson = json, actualizadoEn = ahora) }
        return 1
    }

    // Igual que el UPDATE real: solo cambia si la faena existe y aún no tiene decisión
    override suspend fun guardarDecision(id: String, json: String, ahora: Long): Int {
        val actual = filas.value[id]
        if (actual == null || actual.decisionJson != null) return 0
        cambiar(id) { it.copy(decisionJson = json, actualizadoEn = ahora) }
        return 1
    }
}

// Anota qué se guardó y permite simular un fallo de disco.
class RegistroMemoria : RegistroFaena {
    val llamadas = mutableListOf<String>()
    var fallar = false

    override suspend fun guardarChecklist(id: String, checklist: ChecklistFaena) {
        check(!fallar); llamadas += "checklist:$id"
    }
    override suspend fun guardarAst(id: String, ast: FormularioAst) {
        check(!fallar); llamadas += "ast:$id"
    }
    override suspend fun guardarEncuesta(id: String, encuesta: EncuestaSalud) {
        check(!fallar); llamadas += "encuesta:$id"
    }
    override suspend fun guardarDecision(id: String, decision: RegistroDecision) {
        check(!fallar); llamadas += "decision:$id"
    }
}
