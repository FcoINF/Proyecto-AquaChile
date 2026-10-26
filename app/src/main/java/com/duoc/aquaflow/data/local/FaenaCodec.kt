package com.duoc.aquaflow.data.local

import com.duoc.aquaflow.model.AntecedentesAst
import com.duoc.aquaflow.model.ChecklistFaena
import com.duoc.aquaflow.model.Decision
import com.duoc.aquaflow.model.EncuestaSalud
import com.duoc.aquaflow.model.EstadoEquipo
import com.duoc.aquaflow.model.FormularioAst
import com.duoc.aquaflow.model.ItemEquipamiento
import com.duoc.aquaflow.model.NivelSalud
import com.duoc.aquaflow.model.PasoAst
import com.duoc.aquaflow.model.RegistroCierre
import com.duoc.aquaflow.model.RegistroDecision
import com.duoc.aquaflow.model.ResultadoAst
import com.duoc.aquaflow.model.EstadoSeccion
import com.duoc.aquaflow.domain.ReglasSecciones
import com.duoc.aquaflow.model.ChecklistTareas
import com.duoc.aquaflow.model.TareaFaena
import com.duoc.aquaflow.model.RegistroInicio
import org.json.JSONArray
import org.json.JSONObject

// Convierte cada parte de la faena a texto JSON y de vuelta, para guardarla en una columna de Room.
// Al leer, un texto dañado devuelve null en vez de botar la app: la parte queda "sin registrar".
object FaenaCodec {

    fun checklistATexto(c: ChecklistFaena): String = JSONObject()
        .put("estado", ReglasSecciones.checklist(c).name)
        .put("idFaena", c.idFaena)
        .put("items", JSONArray().also { arr ->
            c.items.forEach {
                arr.put(
                    JSONObject()
                        .put("id", it.id).put("titulo", it.titulo).put("descripcion", it.descripcion)
                        .put("estado", it.estado.name).put("notas", it.notas)
                        .put("categoria", it.categoria).put("badge", it.badge)
                )
            }
        })
        .put("fotos", JSONArray(c.fotosEvidencia))
        .toString()

    fun checklistDesde(texto: String): ChecklistFaena? = runCatching {
        val o = JSONObject(texto)
        ChecklistFaena(
            idFaena = o.getString("idFaena"),
            items = o.getJSONArray("items").objetos().map {
                ItemEquipamiento(
                    id = it.getString("id"),
                    titulo = it.getString("titulo"),
                    descripcion = it.getString("descripcion"),
                    estado = EstadoEquipo.valueOf(it.getString("estado")),
                    notas = it.optString("notas", ""),
                    categoria = it.optString("categoria", ""),
                    badge = it.optString("badge", "")
                )
            },
            fotosEvidencia = o.getJSONArray("fotos").textos(),
            estado = estadoDesde(o)
        )
    }.getOrNull()?.let { it.copy(estado = ReglasSecciones.checklist(it)) }

    fun astATexto(a: FormularioAst): String = JSONObject()
        .put("estado", ReglasSecciones.ast(a).name)
        .put("versionPreguntas", a.versionPreguntas)
        .put("antecedentes", JSONObject()
            .put("fechaInicio", a.antecedentes.fechaInicio)
            .put("turnoInicio", a.antecedentes.turnoInicio)
            .put("fechaCierre", a.antecedentes.fechaCierre)
            .put("turnoCierre", a.antecedentes.turnoCierre)
            .put("descripcion", a.antecedentes.descripcion)
            .put("ubicacion", a.antecedentes.ubicacion)
            .put("ejecucion", a.antecedentes.ejecucion)
            .put("empresa", a.antecedentes.empresa)
            .put("rutEmpresa", a.antecedentes.rutEmpresa)
            .put("otraTipologia", a.antecedentes.otraTipologia)
        )
        .put("tipologia", a.tipologia)
        .put("requiereNuevoAst", a.requiereNuevoAst)
        .put("respuestas", JSONArray(a.respuestas))
        .put("pasos", JSONArray().also { arr ->
            a.pasos.forEach {
                arr.put(
                    JSONObject()
                        .put("tarea", it.tarea).put("peligro", it.peligro)
                        .put("control", it.control).put("critico", it.critico)
                        .put("fechasRevision", JSONArray(it.fechasRevision))
                )
            }
        })
        .put("resultado", a.resultado.name)
        .toString()

    fun astDesde(texto: String): FormularioAst? = runCatching {
        val o = JSONObject(texto)
        val respuestas = o.getJSONArray("respuestas")
        FormularioAst(
            tipologia = o.getString("tipologia"),
            versionPreguntas = o.optInt("versionPreguntas", 1),
            antecedentes = (o.optJSONObject("antecedentes") ?: JSONObject()).let { datos ->
                AntecedentesAst(
                    fechaInicio = datos.optString("fechaInicio", ""),
                    turnoInicio = datos.optString("turnoInicio", ""),
                    fechaCierre = datos.optString("fechaCierre", ""),
                    turnoCierre = datos.optString("turnoCierre", ""),
                    descripcion = datos.optString("descripcion", ""),
                    ubicacion = datos.optString("ubicacion", ""),
                    ejecucion = datos.optString("ejecucion", ""),
                    empresa = datos.optString("empresa", ""),
                    rutEmpresa = datos.optString("rutEmpresa", ""),
                    otraTipologia = datos.optString("otraTipologia", "")
                )
            },
            requiereNuevoAst = o.getBoolean("requiereNuevoAst"),
            respuestas = (0 until respuestas.length()).map { respuestas.getBoolean(it) },
            pasos = o.getJSONArray("pasos").objetos().map {
                PasoAst(
                    tarea = it.optString("tarea", ""),
                    peligro = it.optString("peligro", ""),
                    control = it.optString("control", ""),
                    critico = it.optBoolean("critico", false),
                    fechasRevision = it.optJSONArray("fechasRevision")?.textos() ?: List(4) { "" }
                )
            },
            resultado = ResultadoAst.valueOf(o.getString("resultado")),
            estado = estadoDesde(o)
        )
    }.getOrNull()?.let { it.copy(estado = ReglasSecciones.ast(it)) }

    fun encuestaATexto(e: EncuestaSalud): String = JSONObject()
        .put("estado", ReglasSecciones.encuesta(e).name)
        .put("sistolica", e.sistolica).put("diastolica", e.diastolica).put("saturacion", e.saturacion)
        .put("alertas", JSONArray(e.alertasEncuesta))
        .put("nivel", e.nivel.name)
        .toString()

    fun encuestaDesde(texto: String): EncuestaSalud? = runCatching {
        val o = JSONObject(texto)
        EncuestaSalud(
            sistolica = o.getInt("sistolica"),
            diastolica = o.getInt("diastolica"),
            saturacion = o.getInt("saturacion"),
            alertasEncuesta = o.getJSONArray("alertas").textos(),
            nivel = NivelSalud.valueOf(o.getString("nivel")),
            estado = estadoDesde(o)
        )
    }.getOrNull()?.let { it.copy(estado = ReglasSecciones.encuesta(it)) }

    fun decisionATexto(d: RegistroDecision): String = JSONObject()
        .put("decision", d.decision.name).put("motivo", d.motivo).put("decididoPor", d.decididoPor)
        .toString()

    fun decisionDesde(texto: String): RegistroDecision? = runCatching {
        val o = JSONObject(texto)
        RegistroDecision(
            decision = Decision.valueOf(o.getString("decision")),
            motivo = o.optString("motivo", ""),
            decididoPor = o.getString("decididoPor")
        )
    }.getOrNull()

    fun cierreATexto(c: RegistroCierre): String = JSONObject()
        .put("duracionMinutos", c.duracionMinutos).put("observaciones", c.observaciones)
        .put("revisionSalida", c.revisionSalida).put("cerradoPor", c.cerradoPor)
        .put("cerradoEn", c.cerradoEn)
        .put("saludSalida", c.saludSalida?.let { JSONObject(encuestaATexto(it)) }).toString()

    fun cierreDesde(texto: String): RegistroCierre? = runCatching {
        val o = JSONObject(texto)
        RegistroCierre(o.getInt("duracionMinutos"), o.getString("observaciones"),
            o.getBoolean("revisionSalida"), o.getString("cerradoPor"), o.getLong("cerradoEn"),
            o.optJSONObject("saludSalida")?.let { encuestaDesde(it.toString()) })
    }.getOrNull()

    fun tareasATexto(t: ChecklistTareas): String = JSONObject()
        .put("estado", ReglasSecciones.tareas(t).name)
        .put("items", JSONArray().also { arr -> t.items.forEach {
            arr.put(JSONObject().put("descripcion", it.descripcion).put("verificada", it.verificada)
                .put("peligro", it.peligro).put("control", it.control)
                .put("fechasRevision", JSONArray(it.fechasRevision)))
        } }).toString()

    fun tareasDesde(texto: String): ChecklistTareas? = runCatching {
        val o = JSONObject(texto)
        ChecklistTareas(o.getJSONArray("items").objetos().map {
            TareaFaena(it.getString("descripcion"), it.getBoolean("verificada"),
                it.optString("peligro", ""), it.optString("control", ""),
                it.optJSONArray("fechasRevision")?.textos() ?: List(4) { "" })
        }, estadoDesde(o))
    }.getOrNull()?.let { it.copy(estado = ReglasSecciones.tareas(it)) }

    fun inicioATexto(i: RegistroInicio): String = JSONObject()
        .put("iniciadoPor", i.iniciadoPor).put("iniciadoEn", i.iniciadoEn).toString()

    fun inicioDesde(texto: String): RegistroInicio? = runCatching {
        val o = JSONObject(texto)
        RegistroInicio(o.getString("iniciadoPor"), o.getLong("iniciadoEn"))
    }.getOrNull()

    // develop guardaba también borradores sin estado: los registros antiguos necesitan reconfirmación.
    private fun estadoDesde(o: JSONObject): EstadoSeccion =
        if (!o.has("estado")) EstadoSeccion.BORRADOR
        else runCatching { EstadoSeccion.valueOf(o.getString("estado")) }.getOrDefault(EstadoSeccion.BORRADOR)

    private fun JSONArray.objetos(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
    private fun JSONArray.textos(): List<String> = (0 until length()).map { getString(it) }
}
