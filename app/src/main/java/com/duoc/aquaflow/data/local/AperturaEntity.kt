package com.duoc.aquaflow.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.duoc.aquaflow.model.FaenaGuardada
import com.duoc.aquaflow.model.PreChequeo
import com.duoc.aquaflow.model.ResumenFaena

// Una fila por faena: la apertura en columnas y cada sección (checklist, AST, salud, decisión)
// como texto JSON. Una sección en null significa que todavía no se registra.
@Entity(tableName = "aperturas")
data class AperturaEntity(
    @PrimaryKey val id: String,
    val centroCultivo: String,
    val fecha: String,
    val hora: String,
    val buzoAsignado: String,
    val supervisorACargo: String,
    val condicionesListas: Boolean = false,
    val embarcacionLista: Boolean? = null,
    val comunicacionesOperativas: Boolean? = null,
    val actualizadoEn: Long,
    val checklistJson: String? = null,
    val astJson: String? = null,
    val encuestaJson: String? = null,
    val decisionJson: String? = null,
    val cierreJson: String? = null,
    val inicioJson: String? = null
) {
    fun aModelo() = PreChequeo(
        id, centroCultivo, fecha, hora, buzoAsignado, supervisorACargo,
        condicionesListas, embarcacionLista, comunicacionesOperativas
    )

    fun aFaena() = FaenaGuardada(
        resumen = ResumenFaena(
            apertura = aModelo(),
            checklist = checklistJson?.let(FaenaCodec::checklistDesde),
            ast = astJson?.let(FaenaCodec::astDesde),
            encuesta = encuestaJson?.let(FaenaCodec::encuestaDesde)
        ),
        decision = decisionJson?.let(FaenaCodec::decisionDesde),
        cierre = cierreJson?.let(FaenaCodec::cierreDesde),
        inicio = inicioJson?.let(FaenaCodec::inicioDesde)
    )

    companion object {
        fun desde(apertura: PreChequeo, actualizadoEn: Long) = AperturaEntity(
            apertura.id, apertura.centroCultivo, apertura.fecha, apertura.hora,
            apertura.buzoAsignado, apertura.supervisorACargo,
            apertura.condicionesListas, apertura.embarcacionLista, apertura.comunicacionesOperativas,
            actualizadoEn
        )
    }
}

internal fun AperturaEntity.mismosDatosQue(otra: AperturaEntity) =
    centroCultivo == otra.centroCultivo && fecha == otra.fecha && hora == otra.hora &&
        buzoAsignado == otra.buzoAsignado && supervisorACargo == otra.supervisorACargo &&
        condicionesListas == otra.condicionesListas && embarcacionLista == otra.embarcacionLista &&
        comunicacionesOperativas == otra.comunicacionesOperativas
