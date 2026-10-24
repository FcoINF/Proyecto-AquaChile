package com.duoc.aquaflow.data.local

import com.duoc.aquaflow.model.ChecklistFaena
import com.duoc.aquaflow.model.Decision
import com.duoc.aquaflow.model.EncuestaSalud
import com.duoc.aquaflow.model.EstadoEquipo
import com.duoc.aquaflow.model.FormularioAst
import com.duoc.aquaflow.model.ItemEquipamiento
import com.duoc.aquaflow.model.NivelSalud
import com.duoc.aquaflow.model.PasoAst
import com.duoc.aquaflow.model.RegistroDecision
import com.duoc.aquaflow.model.ResultadoAst
import org.json.JSONArray
import org.json.JSONObject

// Convierte cada parte de la faena a texto JSON y de vuelta, para guardarla en una columna de Room.
// Al leer, un texto dañado devuelve null en vez de botar la app: la parte queda "sin registrar".
object FaenaCodec {

    fun checklistATexto(c: ChecklistFaena): String = JSONObject()
        .put("idFaena", c.idFaena)
        .put("items", JSONArray().also { arr ->
            c.items.forEach {
                arr.put(
                    JSONObject()
                        .put("id", it.id).put("titulo", it.titulo).put("descripcion", it.descripcion)
                        .put("estado", it.estado.name).put("notas", it.notas)
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
                    notas = it.optString("notas", "")
                )
            },
            fotosEvidencia = o.getJSONArray("fotos").textos()
        )
    }.getOrNull()

    fun astATexto(a: FormularioAst): String = JSONObject()
        .put("tipologia", a.tipologia)
        .put("requiereNuevoAst", a.requiereNuevoAst)
        .put("respuestas", JSONArray(a.respuestas))
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
            respuestas = (0 until respuestas.length()).map { respuestas.getBoolean(it) },
            pasos = o.getJSONArray("pasos").objetos().map {
                PasoAst(
                    tarea = it.optString("tarea", ""),
                    peligro = it.optString("peligro", ""),
                    control = it.optString("control", ""),
                    critico = it.optBoolean("critico", false)
                )
            },
            resultado = ResultadoAst.valueOf(o.getString("resultado"))
        )
    }.getOrNull()

    fun encuestaATexto(e: EncuestaSalud): String = JSONObject()
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
            nivel = NivelSalud.valueOf(o.getString("nivel"))
        )
    }.getOrNull()

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

    private fun JSONArray.objetos(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
    private fun JSONArray.textos(): List<String> = (0 until length()).map { getString(it) }
}
