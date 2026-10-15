package com.duoc.aquaflow.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Modelos persistidos (Room). Los enums se guardan por nombre.
 * Los datos de usuarios, buzos y centros son ficticios (requisito no funcional 2).
 */

@Entity(tableName = "usuarios", indices = [Index(value = ["rut"], unique = true)])
data class Usuario(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    /** RUT normalizado: cuerpo-dígito verificador, sin puntos (ej. 12345678-5). */
    val rut: String,
    val rol: Rol,
    val pinHash: String,
    val pinSalt: String,
    val activo: Boolean = true,
    val creadoAt: Long
)

@Entity(tableName = "centros")
data class CentroCultivo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val ubicacion: String,
    val activo: Boolean = true
)

@Entity(tableName = "buzos")
data class Buzo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val rut: String,
    val matricula: String,
    val categoria: String,
    /** Último día (LocalDate.toEpochDay) en que la certificación está vigente. */
    val certificacionVigenteHastaEpochDay: Long,
    val activo: Boolean = true
)

/**
 * Una faena de buceo. Los nombres de centro, buzo y supervisor se guardan como "foto" del momento
 * de la apertura para que el expediente histórico no cambie si luego se edita el catálogo.
 */
@Entity(
    tableName = "faenas",
    indices = [Index("supervisorId"), Index("buzoId"), Index("estado")]
)
data class Faena(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val codigo: String,
    val centroId: Long,
    val centroNombre: String,
    val buzoId: Long,
    val buzoNombre: String,
    val supervisorId: Long,
    val supervisorNombre: String,
    /** Fecha y hora programada de la inmersión (epoch millis). */
    val fechaInicio: Long,
    val estado: EstadoFaena,
    val supervisorFirmaAt: Long? = null,
    val decisionPorId: Long? = null,
    val decisionPorNombre: String? = null,
    val decisionAt: Long? = null,
    /** Motivo del rechazo o de la cancelación. */
    val decisionMotivo: String? = null,
    val creadoAt: Long,
    val actualizadoAt: Long,
    val estadoSync: EstadoSync = EstadoSync.PENDIENTE
)

@Entity(
    tableName = "respuestas_checklist",
    primaryKeys = ["faenaId", "itemId"],
    foreignKeys = [ForeignKey(
        entity = Faena::class,
        parentColumns = ["id"],
        childColumns = ["faenaId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("faenaId")]
)
data class RespuestaChecklist(
    val faenaId: Long,
    /** Id estable del equipo en [com.duoc.aquaflow.domain.catalog.Catalogos]. */
    val itemId: String,
    val estado: EstadoEquipo,
    val nota: String = ""
)

@Entity(
    tableName = "antecedentes_ast",
    foreignKeys = [ForeignKey(
        entity = Faena::class,
        parentColumns = ["id"],
        childColumns = ["faenaId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class AntecedentesAst(
    @PrimaryKey val faenaId: Long,
    val tipologia: String = "",
    val tarea: String = "",
    val pasos: String = "",
    val profundidadMaxPlanificadaM: Double? = null,
    /** El equipo indica que cambiaron las condiciones y se necesita un nuevo AST. */
    val requiereNuevoAst: Boolean = false,
    val actualizadoAt: Long
)

@Entity(
    tableName = "respuestas_formulario",
    primaryKeys = ["faenaId", "formulario", "clave"],
    foreignKeys = [ForeignKey(
        entity = Faena::class,
        parentColumns = ["id"],
        childColumns = ["faenaId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("faenaId")]
)
data class RespuestaFormulario(
    val faenaId: Long,
    val formulario: TipoFormulario,
    /** Id estable de la pregunta/riesgo en el catálogo. */
    val clave: String,
    /** Gating AST: true = "Sí (apto)". Riesgo: true = medida verificada. Salud: true = declaración favorable. */
    val respuesta: Boolean
)

@Entity(
    tableName = "encuestas_salud",
    foreignKeys = [ForeignKey(
        entity = Faena::class,
        parentColumns = ["id"],
        childColumns = ["faenaId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class EncuestaSalud(
    @PrimaryKey val faenaId: Long,
    val sistolica: Int? = null,
    val diastolica: Int? = null,
    val saturacionO2: Int? = null,
    val temperaturaC: Double? = null,
    val observaciones: String = "",
    val actualizadoAt: Long
)

@Entity(
    tableName = "evidencias",
    foreignKeys = [ForeignKey(
        entity = Faena::class,
        parentColumns = ["id"],
        childColumns = ["faenaId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("faenaId")]
)
data class Evidencia(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val faenaId: Long,
    val tipo: TipoEvidencia,
    /** Equipo del checklist asociado (solo para [TipoEvidencia.EQUIPO]). */
    val itemId: String? = null,
    /** Ruta relativa al directorio de evidencias de la app. */
    val ruta: String,
    val creadoAt: Long
)

@Entity(
    tableName = "registros_posteriores",
    foreignKeys = [ForeignKey(
        entity = Faena::class,
        parentColumns = ["id"],
        childColumns = ["faenaId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class RegistroPosterior(
    @PrimaryKey val faenaId: Long,
    val tiempoInmersionMin: Int,
    val profundidadMaxM: Double,
    val presionBancoBar: Int,
    val sinDolorArticular: Boolean,
    val respiracionNormal: Boolean,
    val observaciones: String = "",
    val registradoPorId: Long,
    val registradoAt: Long
)

@Entity(
    tableName = "eventos_faena",
    foreignKeys = [ForeignKey(
        entity = Faena::class,
        parentColumns = ["id"],
        childColumns = ["faenaId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("faenaId")]
)
data class EventoFaena(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val faenaId: Long,
    val tipo: TipoEvento,
    val detalle: String = "",
    val actorId: Long,
    val actorNombre: String,
    val at: Long
)
