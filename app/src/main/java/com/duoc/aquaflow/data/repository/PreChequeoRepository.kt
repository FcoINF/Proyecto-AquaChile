package com.duoc.aquaflow.data.repository

import com.duoc.aquaflow.model.PreChequeo
import com.duoc.aquaflow.data.local.AperturaEntity
import com.duoc.aquaflow.data.local.PreChequeoDao
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.model.Usuario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface BorradoresApertura {
    suspend fun guardar(apertura: PreChequeo)
    suspend fun recuperar(supervisor: String): PreChequeo?
}

interface ConsultaAperturas {
    fun observar(usuario: Usuario): Flow<List<PreChequeo>>
    fun observarDetalle(id: String, usuario: Usuario): Flow<PreChequeo?>
}

class PreChequeoRepository(private val dao: PreChequeoDao) : BorradoresApertura, ConsultaAperturas {
    override suspend fun guardar(apertura: PreChequeo) {
        require(apertura.id.isNotBlank() && apertura.supervisorACargo.isNotBlank())
        dao.guardar(AperturaEntity.desde(apertura, System.currentTimeMillis()))
    }

    override suspend fun recuperar(supervisor: String): PreChequeo? =
        dao.ultimaApertura(supervisor)?.aModelo()

    override fun observar(usuario: Usuario): Flow<List<PreChequeo>> =
        dao.observarAperturas(supervisorVisible(usuario)).map { lista -> lista.map { it.aModelo() } }

    override fun observarDetalle(id: String, usuario: Usuario): Flow<PreChequeo?> =
        dao.observarApertura(id, supervisorVisible(usuario)).map { it?.aModelo() }

    private fun supervisorVisible(usuario: Usuario): String? = when (usuario.rol) {
        Rol.JEFE -> null
        Rol.SUPERVISOR -> usuario.nombre
    }
}
