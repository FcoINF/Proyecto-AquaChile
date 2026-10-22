package com.duoc.aquaflow

import com.duoc.aquaflow.data.repository.BorradoresApertura
import com.duoc.aquaflow.model.PreChequeo

class BorradoresMemoria : BorradoresApertura {
    var apertura: PreChequeo? = null
    var fallar = false
    override suspend fun guardar(apertura: PreChequeo) {
        check(!fallar)
        this.apertura = apertura
    }
    override suspend fun recuperar(supervisor: String): PreChequeo? {
        check(!fallar)
        return apertura?.takeIf { it.supervisorACargo == supervisor }
    }
}
