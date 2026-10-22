package com.duoc.aquaflow.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.duoc.aquaflow.model.PreChequeo

// Solo persiste la apertura; las demás secciones se integrarán después.
@Entity(tableName = "aperturas")
data class AperturaEntity(
    @PrimaryKey val id: String,
    val centroCultivo: String,
    val fecha: String,
    val hora: String,
    val buzoAsignado: String,
    val supervisorACargo: String,
    val actualizadoEn: Long
) {
    fun aModelo() = PreChequeo(id, centroCultivo, fecha, hora, buzoAsignado, supervisorACargo)

    companion object {
        fun desde(apertura: PreChequeo, actualizadoEn: Long) = AperturaEntity(
            apertura.id, apertura.centroCultivo, apertura.fecha, apertura.hora,
            apertura.buzoAsignado, apertura.supervisorACargo, actualizadoEn
        )
    }
}
