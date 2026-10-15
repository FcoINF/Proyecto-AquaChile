package com.duoc.aquaflow.data.seed

import androidx.room.withTransaction
import com.duoc.aquaflow.data.local.AquaFlowDatabase
import com.duoc.aquaflow.domain.catalog.Catalogos
import com.duoc.aquaflow.domain.rules.PinHasher
import com.duoc.aquaflow.model.AntecedentesAst
import com.duoc.aquaflow.model.Buzo
import com.duoc.aquaflow.model.CentroCultivo
import com.duoc.aquaflow.model.EncuestaSalud
import com.duoc.aquaflow.model.EstadoEquipo
import com.duoc.aquaflow.model.EstadoFaena
import com.duoc.aquaflow.model.EventoFaena
import com.duoc.aquaflow.model.Faena
import com.duoc.aquaflow.model.RegistroPosterior
import com.duoc.aquaflow.model.RespuestaChecklist
import com.duoc.aquaflow.model.RespuestaFormulario
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.model.TipoEvento
import com.duoc.aquaflow.model.TipoFormulario
import com.duoc.aquaflow.model.Usuario
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Datos ficticios de demostración (requisito no funcional 2: no se usan datos reales). */
object SeedData {

    data class UsuarioDemo(val nombre: String, val rut: String, val rol: Rol, val pin: String)

    val usuarios: List<UsuarioDemo> = listOf(
        UsuarioDemo("Carlos Henríquez S.", "15892403-K", Rol.SUPERVISOR, "4921"),
        UsuarioDemo("Rodrigo Valenzuela M.", "14785236-3", Rol.SUPERVISOR, "1357"),
        UsuarioDemo("Rodrigo Alarcón T.", "12433910-3", Rol.ADMINISTRADOR, "2468")
    )

    val centros: List<CentroCultivo> = listOf(
        CentroCultivo(nombre = "Centro Huito", ubicacion = "Balsa Jaula 04 · Pontón Salmones 12"),
        CentroCultivo(nombre = "Centro Calbuco", ubicacion = "Módulo 104 · Seno Reloncaví"),
        CentroCultivo(nombre = "Centro Reloncaví", ubicacion = "Módulo 04 · Jaula Red Lobera 12")
    )

    /** Las vigencias son relativas a [hoy] para que los datos de demostración no caduquen. */
    fun buzos(hoy: LocalDate): List<Buzo> = listOf(
        Buzo(
            nombre = "Héctor Oliver Barría",
            rut = "16162069-6",
            matricula = "334.821-B",
            categoria = "Buzo Comercial Intermedio",
            certificacionVigenteHastaEpochDay = hoy.plusMonths(14).toEpochDay()
        ),
        Buzo(
            nombre = "Matías Soto Ibarra",
            rut = "17654321-3",
            matricula = "351.207-B",
            categoria = "Buzo Comercial Básico",
            certificacionVigenteHastaEpochDay = hoy.plusMonths(6).toEpochDay()
        ),
        Buzo(
            nombre = "Felipe Navarro Cárdenas",
            rut = "13579246-2",
            matricula = "298.440-B",
            categoria = "Buzo Comercial Intermedio",
            certificacionVigenteHastaEpochDay = hoy.plusMonths(20).toEpochDay()
        ),
        // Certificación vencida a propósito: permite probar la regla de asignación de buzos.
        Buzo(
            nombre = "Ignacio Fuentes Paredes",
            rut = "18246810-K",
            matricula = "310.112-B",
            categoria = "Buzo Comercial Básico",
            certificacionVigenteHastaEpochDay = hoy.minusDays(30).toEpochDay()
        )
    )
}

/**
 * Carga los datos iniciales la primera vez. [ensureSeeded] es seguro de llamar desde varios lugares:
 * solo la primera llamada escribe, las demás esperan y retornan.
 */
class DatabaseSeeder(
    private val db: AquaFlowDatabase,
    private val ahora: () -> Long = { System.currentTimeMillis() },
    private val hoy: () -> LocalDate = { LocalDate.now() }
) {
    private val mutex = Mutex()

    @Volatile
    private var listo = false

    @Volatile
    private var faenasDemoListas = false

    /**
     * @param incluirFaenasDemo si es true también siembra faenas de ejemplo en estados
     * terminales (cerradas/rechazada) para que el historial tenga contenido sin cargar
     * datos a mano. Siempre es false en tests: los instrumentados parten de base vacía.
     */
    suspend fun ensureSeeded(incluirFaenasDemo: Boolean = false) {
        if (listo && (!incluirFaenasDemo || faenasDemoListas)) return
        mutex.withLock {
            if (!listo) {
                db.withTransaction {
                    if (db.usuarioDao().count() == 0) {
                        val creado = ahora()
                        db.usuarioDao().insertAll(
                            SeedData.usuarios.map { demo ->
                                val sal = PinHasher.nuevaSal()
                                Usuario(
                                    nombre = demo.nombre,
                                    rut = demo.rut,
                                    rol = demo.rol,
                                    pinHash = PinHasher.hash(demo.pin, sal),
                                    pinSalt = sal,
                                    creadoAt = creado
                                )
                            }
                        )
                    }
                    if (db.centroDao().count() == 0) db.centroDao().insertAll(SeedData.centros)
                    if (db.buzoDao().count() == 0) db.buzoDao().insertAll(SeedData.buzos(hoy()))
                }
                listo = true
            }
            if (incluirFaenasDemo && !faenasDemoListas) {
                db.withTransaction {
                    if (db.faenaDao().count() == 0) sembrarFaenasDemo()
                }
                faenasDemoListas = true
            }
        }
    }

    /**
     * Tres faenas de ejemplo con formularios completos y bitácora, todas en estados
     * terminales para no ocupar a ningún buzo (la regla "un buzo = una faena activa"
     * sigue intacta) y con fechas relativas a hoy para que la demo no caduque.
     */
    private suspend fun sembrarFaenasDemo() {
        val supCarlos = db.usuarioDao().findByRut("15892403-K")!!
        val supRodrigo = db.usuarioDao().findByRut("14785236-3")!!
        val jefe = db.usuarioDao().findByRut("12433910-3")!!
        val centros = db.centroDao().observeActivos().first()
        val buzos = db.buzoDao().observeActivos().first()
        fun centro(nombre: String) = centros.first { it.nombre == nombre }
        fun buzo(nombre: String) = buzos.first { it.nombre == nombre }

        val dia = 24 * 3_600_000L
        val base = ahora()

        // 1) Cerrada con registro: Héctor, supervisor Carlos, hace 6 días.
        sembrarFaenaCerrada(
            supervisor = supCarlos,
            jefe = jefe,
            centro = centro("Centro Huito"),
            buzo = buzo("Héctor Oliver Barría"),
            fechaInicio = base - 6 * dia,
            tiempoMin = 38,
            profundidadM = 22.4,
            presionBar = 110
        )
        // 2) Cerrada con registro: Matías, supervisor Rodrigo, hace 12 días.
        sembrarFaenaCerrada(
            supervisor = supRodrigo,
            jefe = jefe,
            centro = centro("Centro Calbuco"),
            buzo = buzo("Matías Soto Ibarra"),
            fechaInicio = base - 12 * dia,
            tiempoMin = 42,
            profundidadM = 18.2,
            presionBar = 95
        )
        // 3) Rechazada por equipo: Felipe, supervisor Carlos, hace 2 días.
        sembrarFaenaRechazada(
            supervisor = supCarlos,
            jefe = jefe,
            centro = centro("Centro Reloncaví"),
            buzo = buzo("Felipe Navarro Cárdenas"),
            fechaInicio = base - 2 * dia
        )
    }

    private suspend fun formulariosFavorables(faenaId: Long, at: Long, conFalla: Boolean = false) {
        db.checklistDao().upsertAll(
            Catalogos.equipos.map {
                val estado = if (conFalla && it.id == "EQ_COMPRESOR") EstadoEquipo.NO_CUMPLE else EstadoEquipo.CUMPLE
                val nota = if (estado == EstadoEquipo.NO_CUMPLE) "Manómetro sin presión de trabajo" else ""
                RespuestaChecklist(faenaId, it.id, estado, nota)
            }
        )
        db.antecedentesAstDao().upsert(
            AntecedentesAst(
                faenaId = faenaId,
                tipologia = "Inspección de fondo y fondeos",
                tarea = "Revisión de fondeos y paños de red",
                pasos = "1. Descenso por cabo guía\n2. Inspección\n3. Ascenso",
                profundidadMaxPlanificadaM = 25.0,
                actualizadoAt = at
            )
        )
        val gating = Catalogos.gatingAst.map { RespuestaFormulario(faenaId, TipoFormulario.AST_GATING, it.id, true) }
        val riesgos = Catalogos.riesgosAst.map { RespuestaFormulario(faenaId, TipoFormulario.AST_RIESGO, it.id, true) }
        val salud = Catalogos.declaracionSalud.map { RespuestaFormulario(faenaId, TipoFormulario.SALUD_DECLARACION, it.id, true) }
        db.respuestaFormularioDao().upsertAll(gating + riesgos + salud)
        db.encuestaSaludDao().upsert(
            EncuestaSalud(
                faenaId = faenaId,
                sistolica = 120,
                diastolica = 80,
                saturacionO2 = 98,
                temperaturaC = 36.6,
                actualizadoAt = at
            )
        )
    }

    private suspend fun evento(
        faenaId: Long,
        tipo: TipoEvento,
        actor: Usuario,
        at: Long,
        detalle: String = ""
    ) {
        db.eventoFaenaDao().insert(EventoFaena(faenaId = faenaId, tipo = tipo, detalle = detalle, actorId = actor.id, actorNombre = actor.nombre, at = at))
    }

    private fun codigoFaena(fechaInicio: Long, id: Long): String {
        val anio = Instant.ofEpochMilli(fechaInicio).atZone(ZoneId.systemDefault()).year
        return "F-$anio-" + id.toString().padStart(4, '0')
    }

    private suspend fun sembrarFaenaCerrada(
        supervisor: Usuario,
        jefe: Usuario,
        centro: CentroCultivo,
        buzo: Buzo,
        fechaInicio: Long,
        tiempoMin: Int,
        profundidadM: Double,
        presionBar: Int
    ) {
        val creado = fechaInicio - 3_600_000L
        val firma = fechaInicio + 3_600_000L
        val decision = fechaInicio + 2 * 3_600_000L
        val cierre = fechaInicio + 24 * 3_600_000L
        val id = db.faenaDao().insert(
            Faena(
                codigo = "",
                centroId = centro.id,
                centroNombre = centro.nombre,
                buzoId = buzo.id,
                buzoNombre = buzo.nombre,
                supervisorId = supervisor.id,
                supervisorNombre = supervisor.nombre,
                fechaInicio = fechaInicio,
                estado = EstadoFaena.CERRADA,
                supervisorFirmaAt = firma,
                decisionPorId = jefe.id,
                decisionPorNombre = jefe.nombre,
                decisionAt = decision,
                creadoAt = creado,
                actualizadoAt = cierre
            )
        )
        val base = db.faenaDao().findById(id)!!
        db.faenaDao().update(base.copy(codigo = codigoFaena(fechaInicio, id)))
        formulariosFavorables(id, creado + 600_000L)
        db.registroPosteriorDao().upsert(
            RegistroPosterior(
                faenaId = id,
                tiempoInmersionMin = tiempoMin,
                profundidadMaxM = profundidadM,
                presionBancoBar = presionBar,
                sinDolorArticular = true,
                respiracionNormal = true,
                observaciones = "Sin novedades",
                registradoPorId = supervisor.id,
                registradoAt = cierre
            )
        )
        evento(id, TipoEvento.APERTURA, supervisor, creado)
        evento(id, TipoEvento.CHECKLIST_GUARDADO, supervisor, creado + 600_000L)
        evento(id, TipoEvento.AST_GUARDADO, supervisor, creado + 1_200_000L)
        evento(id, TipoEvento.SALUD_GUARDADA, supervisor, creado + 1_800_000L)
        evento(id, TipoEvento.ENVIADA_A_REVISION, supervisor, firma)
        evento(id, TipoEvento.AUTORIZADA, jefe, decision)
        evento(id, TipoEvento.CERRADA, supervisor, cierre, "Sin novedades")
    }

    private suspend fun sembrarFaenaRechazada(
        supervisor: Usuario,
        jefe: Usuario,
        centro: CentroCultivo,
        buzo: Buzo,
        fechaInicio: Long
    ) {
        val motivo = "Compresor sin presión de trabajo, se reprograma la faena"
        val creado = fechaInicio - 3_600_000L
        val firma = fechaInicio + 3_600_000L
        val decision = fechaInicio + 2 * 3_600_000L
        val id = db.faenaDao().insert(
            Faena(
                codigo = "",
                centroId = centro.id,
                centroNombre = centro.nombre,
                buzoId = buzo.id,
                buzoNombre = buzo.nombre,
                supervisorId = supervisor.id,
                supervisorNombre = supervisor.nombre,
                fechaInicio = fechaInicio,
                estado = EstadoFaena.RECHAZADA,
                supervisorFirmaAt = firma,
                decisionPorId = jefe.id,
                decisionPorNombre = jefe.nombre,
                decisionAt = decision,
                decisionMotivo = motivo,
                creadoAt = creado,
                actualizadoAt = decision
            )
        )
        val base = db.faenaDao().findById(id)!!
        db.faenaDao().update(base.copy(codigo = codigoFaena(fechaInicio, id)))
        formulariosFavorables(id, creado + 600_000L, conFalla = true)
        evento(id, TipoEvento.APERTURA, supervisor, creado)
        evento(id, TipoEvento.CHECKLIST_GUARDADO, supervisor, creado + 600_000L)
        evento(id, TipoEvento.AST_GUARDADO, supervisor, creado + 1_200_000L)
        evento(id, TipoEvento.SALUD_GUARDADA, supervisor, creado + 1_800_000L)
        evento(id, TipoEvento.ENVIADA_A_REVISION, supervisor, firma)
        evento(id, TipoEvento.RECHAZADA, jefe, decision, motivo)
    }
}
