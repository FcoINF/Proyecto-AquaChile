package com.duoc.aquaflow.data.local

import com.duoc.aquaflow.model.PreChequeo

interface PreChequeoDao {
    fun obtenerPreChequeos(): List<PreChequeo>
}