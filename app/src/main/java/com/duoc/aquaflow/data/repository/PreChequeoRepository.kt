package com.duoc.aquaflow.data.repository

import com.duoc.aquaflow.model.PreChequeo

class PreChequeoRepository {
    private var preChequeoLocal: PreChequeo = PreChequeo()

    fun guardarPreChequeo(preChequeo: PreChequeo) {
        preChequeoLocal = preChequeo
    }
}