package com.duoc.aquaflow.data.repository

import com.duoc.aquaflow.model.PreChequeo
import com.duoc.aquaflow.data.local.AperturaEntity
import com.duoc.aquaflow.data.local.PreChequeoDao

interface BorradoresApertura {
    suspend fun guardar(apertura: PreChequeo)
    suspend fun recuperar(supervisor: String): PreChequeo?
}

class PreChequeoRepository(private val dao: PreChequeoDao) : BorradoresApertura {
    override suspend fun guardar(apertura: PreChequeo) {
        require(apertura.id.isNotBlank() && apertura.supervisorACargo.isNotBlank())
        dao.guardar(AperturaEntity.desde(apertura, System.currentTimeMillis()))
    }

    override suspend fun recuperar(supervisor: String): PreChequeo? =
        dao.ultimaApertura(supervisor)?.aModelo()
}
