package com.duoc.aquaflow

import com.duoc.aquaflow.domain.catalog.Catalogos
import com.duoc.aquaflow.domain.rules.PinHasher
import com.duoc.aquaflow.model.AntecedentesAst
import com.duoc.aquaflow.model.EncuestaSalud
import com.duoc.aquaflow.model.EstadoEquipo
import com.duoc.aquaflow.model.EstadoFaena
import com.duoc.aquaflow.model.Faena
import com.duoc.aquaflow.model.FaenaDetalle
import com.duoc.aquaflow.model.RespuestaChecklist
import com.duoc.aquaflow.model.RespuestaFormulario
import com.duoc.aquaflow.model.Rol
import com.duoc.aquaflow.model.TipoFormulario
import com.duoc.aquaflow.model.Usuario

/** Constructores de datos de prueba compartidos por los tests unitarios. */
object TestData {
    const val AHORA = 1_800_000_000_000L
    private const val SAL = "00112233445566778899aabbccddeeff"

    fun usuario(
        id: Long = 1,
        rol: Rol = Rol.SUPERVISOR,
        activo: Boolean = true,
        rut: String = "15892403-K",
        pin: String = "1234"
    ) = Usuario(
        id = id,
        nombre = "Usuario $id",
        rut = rut,
        rol = rol,
        pinHash = PinHasher.hash(pin, SAL),
        pinSalt = SAL,
        activo = activo,
        creadoAt = AHORA
    )

    fun faena(
        id: Long = 10,
        supervisorId: Long = 1,
        estado: EstadoFaena = EstadoFaena.PRECHEQUEO,
        fechaInicio: Long = AHORA,
        buzoNombre: String = "Héctor Oliver Barría",
        centroNombre: String = "Centro Huito"
    ) = Faena(
        id = id,
        codigo = "F-2026-%04d".format(id),
        centroId = 1,
        centroNombre = centroNombre,
        buzoId = 1,
        buzoNombre = buzoNombre,
        supervisorId = supervisorId,
        supervisorNombre = "Usuario $supervisorId",
        fechaInicio = fechaInicio,
        estado = estado,
        creadoAt = AHORA,
        actualizadoAt = AHORA
    )

    fun checklistCompleto(faenaId: Long = 10, estado: EstadoEquipo = EstadoEquipo.CUMPLE) =
        Catalogos.equipos.map { RespuestaChecklist(faenaId, it.id, estado) }

    fun ast(faenaId: Long = 10) = AntecedentesAst(
        faenaId = faenaId,
        tipologia = "Inspección de fondo y fondeos",
        tarea = "Revisión de fondeos",
        pasos = "1. Descenso\n2. Inspección\n3. Ascenso",
        profundidadMaxPlanificadaM = 20.0,
        actualizadoAt = AHORA
    )

    fun respuestasFormulariosCompletas(faenaId: Long = 10, gatingApto: Boolean = true, saludFavorable: Boolean = true) =
        Catalogos.gatingAst.map { RespuestaFormulario(faenaId, TipoFormulario.AST_GATING, it.id, gatingApto) } +
            Catalogos.riesgosAst.map { RespuestaFormulario(faenaId, TipoFormulario.AST_RIESGO, it.id, true) } +
            Catalogos.declaracionSalud.map {
                RespuestaFormulario(faenaId, TipoFormulario.SALUD_DECLARACION, it.id, saludFavorable)
            }

    fun encuesta(
        faenaId: Long = 10,
        sistolica: Int? = 120,
        diastolica: Int? = 80,
        spo2: Int? = 98,
        temperatura: Double? = 36.6
    ) = EncuestaSalud(
        faenaId = faenaId,
        sistolica = sistolica,
        diastolica = diastolica,
        saturacionO2 = spo2,
        temperaturaC = temperatura,
        actualizadoAt = AHORA
    )

    /** Faena con toda la información completa y favorable (lista para firmar/autorizar). */
    fun detalleCompleto(faena: Faena = faena()) = FaenaDetalle(
        faena = faena,
        checklist = checklistCompleto(faena.id),
        antecedentesAst = ast(faena.id),
        respuestas = respuestasFormulariosCompletas(faena.id),
        encuestaSalud = encuesta(faena.id),
        evidencias = emptyList(),
        registro = null,
        eventos = emptyList()
    )

    fun detalleVacio(faena: Faena = faena()) = FaenaDetalle(
        faena = faena,
        checklist = emptyList(),
        antecedentesAst = null,
        respuestas = emptyList(),
        encuestaSalud = null,
        evidencias = emptyList(),
        registro = null,
        eventos = emptyList()
    )
}
