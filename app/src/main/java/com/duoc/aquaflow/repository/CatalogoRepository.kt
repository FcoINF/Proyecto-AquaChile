package com.duoc.aquaflow.repository

import com.duoc.aquaflow.data.local.dao.BuzoDao
import com.duoc.aquaflow.data.local.dao.CentroDao
import com.duoc.aquaflow.model.Buzo
import com.duoc.aquaflow.model.CentroCultivo
import kotlinx.coroutines.flow.Flow

interface CatalogoRepository {
    fun observarCentros(): Flow<List<CentroCultivo>>
    fun observarBuzos(): Flow<List<Buzo>>
}

class RoomCatalogoRepository(
    private val centroDao: CentroDao,
    private val buzoDao: BuzoDao
) : CatalogoRepository {
    override fun observarCentros(): Flow<List<CentroCultivo>> = centroDao.observeActivos()
    override fun observarBuzos(): Flow<List<Buzo>> = buzoDao.observeActivos()
}
