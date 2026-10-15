package com.duoc.aquaflow.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.duoc.aquaflow.data.local.dao.AntecedentesAstDao
import com.duoc.aquaflow.data.local.dao.BuzoDao
import com.duoc.aquaflow.data.local.dao.CentroDao
import com.duoc.aquaflow.data.local.dao.ChecklistDao
import com.duoc.aquaflow.data.local.dao.EncuestaSaludDao
import com.duoc.aquaflow.data.local.dao.EventoFaenaDao
import com.duoc.aquaflow.data.local.dao.EvidenciaDao
import com.duoc.aquaflow.data.local.dao.FaenaDao
import com.duoc.aquaflow.data.local.dao.RegistroPosteriorDao
import com.duoc.aquaflow.data.local.dao.RespuestaFormularioDao
import com.duoc.aquaflow.data.local.dao.UsuarioDao
import com.duoc.aquaflow.model.AntecedentesAst
import com.duoc.aquaflow.model.Buzo
import com.duoc.aquaflow.model.CentroCultivo
import com.duoc.aquaflow.model.EncuestaSalud
import com.duoc.aquaflow.model.EventoFaena
import com.duoc.aquaflow.model.Evidencia
import com.duoc.aquaflow.model.Faena
import com.duoc.aquaflow.model.RegistroPosterior
import com.duoc.aquaflow.model.RespuestaChecklist
import com.duoc.aquaflow.model.RespuestaFormulario
import com.duoc.aquaflow.model.Usuario

@Database(
    entities = [
        Usuario::class,
        CentroCultivo::class,
        Buzo::class,
        Faena::class,
        RespuestaChecklist::class,
        AntecedentesAst::class,
        RespuestaFormulario::class,
        EncuestaSalud::class,
        Evidencia::class,
        RegistroPosterior::class,
        EventoFaena::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AquaFlowDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun centroDao(): CentroDao
    abstract fun buzoDao(): BuzoDao
    abstract fun faenaDao(): FaenaDao
    abstract fun checklistDao(): ChecklistDao
    abstract fun antecedentesAstDao(): AntecedentesAstDao
    abstract fun respuestaFormularioDao(): RespuestaFormularioDao
    abstract fun encuestaSaludDao(): EncuestaSaludDao
    abstract fun evidenciaDao(): EvidenciaDao
    abstract fun registroPosteriorDao(): RegistroPosteriorDao
    abstract fun eventoFaenaDao(): EventoFaenaDao

    companion object {
        const val NOMBRE = "aquaflow.db"

        fun build(context: Context): AquaFlowDatabase =
            Room.databaseBuilder(context.applicationContext, AquaFlowDatabase::class.java, NOMBRE).build()
    }
}
