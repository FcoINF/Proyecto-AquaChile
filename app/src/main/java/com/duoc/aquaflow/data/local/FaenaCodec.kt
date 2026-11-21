package com.duoc.aquaflow.data.local

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
import com.duoc.aquaflow.model.IntentoBloqueado
import com.duoc.aquaflow.model.MedicionesInmersion
import com.duoc.aquaflow.model.MetadatosFoto
import com.duoc.aquaflow.model.OrigenMedicion
import com.duoc.aquaflow.model.PREGUNTAS_SALUD
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
        .put("fotosPorEquipo", JSONObject(c.fotosPorEquipo))
        .put("metadatosPorFoto", JSONObject().also { fotos ->
            c.metadatosPorFoto.forEach { (uri, foto) ->
                fotos.put(uri, JSONObject().put("capturadaEn", foto.capturadaEn)
                    .put("capturadaPor", foto.capturadaPor).put("referencia", foto.referencia)
                    .put("coincide", foto.coincide))
            }
        })
        .toString()

    fun checklistDesde(texto: String): ChecklistFaena? = runCatching {
        val o = JSONObject(texto)
        val items = o.getJSONArray("items").objetos().map {
            ItemEquipamiento(
                id = it.getString("id"),
                titulo = it.getString("titulo"),
                descripcion = it.getString("descripcion"),
                estado = EstadoEquipo.valueOf(it.getString("estado")),
                notas = it.optString("notas", ""),
                categoria = it.optString("categoria", ""),
                badge = it.optString("badge", "")
            )
        }
        val fotos = o.getJSONArray("fotos").textos()
        val fotosPorEquipo = o.optJSONObject("fotosPorEquipo")?.let { asignaciones ->
            asignaciones.keys().asSequence().associateWith { asignaciones.getString(it) }
        }.orEmpty()
        ChecklistFaena(
            idFaena = o.getString("idFaena"),
            items = items,
            fotosEvidencia = fotos,
            estado = estadoDesde(o),
            fotosPorEquipo = fotosPorEquipo,
            metadatosPorFoto = o.optJSONObject("metadatosPorFoto")?.let { fotos ->
                fotos.keys().asSequence().associateWith { uri ->
                    val foto = fotos.getJSONObject(uri)
                    MetadatosFoto(foto.getLong("capturadaEn"), foto.getString("capturadaPor"),
                        foto.optString("referencia", "").takeIf(String::isNotBlank),
                        if (foto.isNull("coincide")) null else foto.getBoolean("coincide"))
                }
            }.orEmpty()
        )
    }.getOrNull()?.let { it.copy(estado = ReglasSecciones.checklist(it)) }

    fun astATexto(a: FormularioAst): String = JSONObject()
        .put("estado", ReglasSecciones.ast(a).name)
        .put("tipologia", a.tipologia)
        .put("requiereNuevoAst", a.requiereNuevoAst)
        .put("respuestas", JSONArray(a.respuestas.map { it ?: JSONObject.NULL }))
        .put("pasos", JSONArray().also { arr ->
            a.pasos.forEach {
                arr.put(
                    JSONObject()
                        .put("tarea", it.tarea).put("peligro", it.peligro)
                        .put("control", it.control).put("critico", it.critico)
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
            requiereNuevoAst = o.getBoolean("requiereNuevoAst"),
            respuestas = (0 until respuestas.length()).map {
                if (respuestas.isNull(it)) null else respuestas.getBoolean(it)
            },
            pasos = o.getJSONArray("pasos").objetos().map {
                PasoAst(
                    tarea = it.optString("tarea", ""),
                    peligro = it.optString("peligro", ""),
                    control = it.optString("control", ""),
                    critico = it.optBoolean("critico", false)
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
        .put("respuestas", JSONObject().also { respuestas ->
            e.respuestas.forEach { (pregunta, respuesta) ->
                respuestas.put(pregunta, respuesta ?: JSONObject.NULL)
            }
        })
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
            estado = estadoDesde(o),
            respuestas = o.optJSONObject("respuestas")?.let { respuestas ->
                respuestas.keys().asSequence().associateWith {
                    if (respuestas.isNull(it)) null else respuestas.getBoolean(it)
                }
            } ?: PREGUNTAS_SALUD.associateWith { null }
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
        .put("saludSalida", c.saludSalida?.let { JSONObject(encuestaATexto(it)) })
        .put("mediciones", c.mediciones?.let {
            JSONObject().put("fondo", it.tiempoFondoMinutos).put("profundidad", it.profundidadMaximaMetros)
                .put("descenso", it.descensoMaximo).put("ascenso", it.ascensoMaximo).put("origen", it.origen.name)
        }).toString()

    fun cierreDesde(texto: String): RegistroCierre? = runCatching {
        val o = JSONObject(texto)
        RegistroCierre(o.getInt("duracionMinutos"), o.getString("observaciones"),
            o.getBoolean("revisionSalida"), o.getString("cerradoPor"), o.getLong("cerradoEn"),
            o.optJSONObject("saludSalida")?.let { encuestaDesde(it.toString()) },
            o.optJSONObject("mediciones")?.let {
                MedicionesInmersion(it.getInt("fondo"), it.getDouble("profundidad"), it.getDouble("descenso"),
                    it.getDouble("ascenso"), OrigenMedicion.valueOf(it.getString("origen")))
            })
    }.getOrNull()

    fun tareasATexto(t: ChecklistTareas): String = JSONObject()
        .put("estado", ReglasSecciones.tareas(t).name)
        .put("items", JSONArray().also { arr -> t.items.forEach {
            arr.put(JSONObject().put("descripcion", it.descripcion).put("verificada", it.verificada))
        } }).toString()

    fun tareasDesde(texto: String): ChecklistTareas? = runCatching {
        val o = JSONObject(texto)
        ChecklistTareas(o.getJSONArray("items").objetos().map {
            TareaFaena(it.getString("descripcion"), it.getBoolean("verificada"))
        }, estadoDesde(o))
    }.getOrNull()?.let { it.copy(estado = ReglasSecciones.tareas(it)) }

    fun inicioATexto(i: RegistroInicio): String = JSONObject()
        .put("iniciadoPor", i.iniciadoPor).put("iniciadoEn", i.iniciadoEn).toString()

    fun inicioDesde(texto: String): RegistroInicio? = runCatching {
        val o = JSONObject(texto)
        RegistroInicio(o.getString("iniciadoPor"), o.getLong("iniciadoEn"))
    }.getOrNull()

    fun intentosBloqueadosATexto(intentos: List<IntentoBloqueado>): String = JSONArray().also { array ->
        intentos.forEach { intento ->
            array.put(JSONObject()
                .put("responsable", intento.responsable)
                .put("fecha", intento.fecha)
                .put("motivo", intento.motivo)
                .put("accion", intento.accion))
        }
    }.toString()

    fun intentosBloqueadosDesde(texto: String): List<IntentoBloqueado> = runCatching {
        val array = JSONArray(texto)
        (0 until array.length()).map { indice ->
            array.getJSONObject(indice).let {
                IntentoBloqueado(it.getString("responsable"), it.getLong("fecha"),
                    it.getString("motivo"), it.getString("accion"))
            }
        }
    }.getOrDefault(emptyList())

    // develop guardaba también borradores sin estado: los registros antiguos necesitan reconfirmación.
    private fun estadoDesde(o: JSONObject): EstadoSeccion =
        if (!o.has("estado")) EstadoSeccion.BORRADOR
        else runCatching { EstadoSeccion.valueOf(o.getString("estado")) }.getOrDefault(EstadoSeccion.BORRADOR)

    private fun JSONArray.objetos(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
    private fun JSONArray.textos(): List<String> = (0 until length()).map { getString(it) }
}
