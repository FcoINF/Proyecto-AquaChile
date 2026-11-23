package com.duoc.aquaflow.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "envios_faena", primaryKeys = ["faena"])
data class EnvioFaena(
    val faena: String,
    val cuenta: String,
    val servidor: String,
    val operacion: String,
    val formulario: String,
    val estado: String = EstadoEnvio.PENDIENTE.name,
    val error: String? = null
)

enum class EstadoEnvio { PENDIENTE, SINCRONIZADO, ERROR }

@Entity(tableName = "fragmentos_foto", primaryKeys = ["faena", "ruta", "orden"], foreignKeys = [
    ForeignKey(entity = EnvioFaena::class, parentColumns = ["faena"], childColumns = ["faena"], onDelete = ForeignKey.CASCADE)
])
data class FragmentoFoto(val faena: String, val ruta: String, val orden: Int, val bytes: ByteArray)

@Dao
interface SincronizacionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(envio: EnvioFaena)

    @Insert
    suspend fun guardarFragmento(fragmento: FragmentoFoto)

    @Query("SELECT * FROM envios_faena WHERE faena = :id")
    suspend fun buscar(id: String): EnvioFaena?

    @Query("SELECT * FROM envios_faena WHERE cuenta = :cuenta AND servidor = :servidor AND estado != 'SINCRONIZADO'")
    suspend fun pendientes(cuenta: String, servidor: String): List<EnvioFaena>

    @Query("SELECT * FROM envios_faena WHERE cuenta = :cuenta AND servidor = :servidor")
    fun observar(cuenta: String, servidor: String): Flow<List<EnvioFaena>>

    @Query("SELECT COUNT(*) FROM fragmentos_foto WHERE faena = :id AND ruta = :ruta")
    suspend fun cantidadFragmentos(id: String, ruta: String): Int

    @Query("SELECT bytes FROM fragmentos_foto WHERE faena = :id AND ruta = :ruta AND orden = :orden")
    suspend fun fragmento(id: String, ruta: String, orden: Int): ByteArray

    @Query("UPDATE envios_faena SET estado = :estado, error = :error WHERE faena = :id AND operacion = :operacion")
    suspend fun actualizar(id: String, operacion: String, estado: String, error: String? = null): Int

    @Query("DELETE FROM fragmentos_foto WHERE faena = :id")
    suspend fun liberarFotos(id: String)
}
