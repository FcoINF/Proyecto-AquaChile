package com.duoc.aquaflow.backend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static com.duoc.aquaflow.backend.ExpedienteDatos.*;

@Service
class Expedientes {
    public record Expediente(Faenas.Apertura apertura, long version, ExpedienteDatos prechequeo,
                             List<Foto> fotos, Decision decision, Inicio inicio, Bitacora bitacora,
                             SemaforoFaena.Evaluacion semaforo, boolean publicado) {}
    public record Foto(UUID faena, Equipo equipo, TipoFoto tipo, String formato,
                       OffsetDateTime tomadaEn, String responsable) {}
    record Imagen(Foto foto, byte[] bytes) {}
    public record Inicio(OffsetDateTime registradaEn, String responsable) {}
    public record Alerta(TipoAlerta tipo, double velocidad, double limite, long detectadaEn, String responsable,
                         OffsetDateTime revisadaEn, String revisadaPor) {}
    public record Bitacora(UUID faena, String buzoId, int duracionMinutos, String observaciones,
                           String responsable, Salud saludSalida, CierreHistorico mediciones, List<Alerta> alertas) {}

    private record Fila(long version, ExpedienteDatos datos, Decision decision, Inicio inicio, boolean publicado) {}

    private final JdbcClient jdbc;
    private final ObjectMapper json;
    private final Faenas faenas;
    private final SemaforoFaena semaforo;

    Expedientes(JdbcClient jdbc, ObjectMapper json, Faenas faenas, SemaforoFaena semaforo) {
        this.jdbc = jdbc;
        this.json = json;
        this.faenas = faenas;
        this.semaforo = semaforo;
    }

    @Transactional
    public Expediente consultar(UUID id, String supervisor) {
        faenas.bloquear(id, supervisor);
        return expediente(id, supervisor);
    }

    @Transactional
    public Expediente guardar(UUID id, String supervisor, long version, ExpedienteDatos datos, boolean publicar) {
        faenas.bloquear(id, supervisor);
        var apertura = faenas.consultar(id, supervisor);
        exigir(apertura.cierre() == null, "La faena ya tiene un cierre.");
        var anterior = buscar(id);
        var equipos = EnumSet.noneOf(Equipo.class);
        for (Inspeccion inspeccion : datos.checklist()) {
            if (!equipos.add(inspeccion.equipo())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cada equipo debe aparecer una sola vez.");
            }
        }
        var fotos = fotos(id);
        for (Inspeccion inspeccion : datos.checklist()) {
            if (inspeccion.coincideConReferencia() != null) {
                exigir(tieneFoto(fotos, inspeccion.equipo(), TipoFoto.CAPTURA)
                                && tieneFoto(fotos, inspeccion.equipo(), TipoFoto.REFERENCIA),
                        "La comparación requiere una captura y una referencia para " + inspeccion.equipo() + ".");
            }
        }
        if (anterior == null) {
            exigir(version == 0, "Para crear el expediente usa version=0.");
            String buzo = apertura.datos().buzoId();
            boolean activo = jdbc.sql("SELECT activo FROM buzos WHERE id = :id FOR UPDATE")
                    .param("id", buzo).query(Boolean.class).optional().orElse(false);
            exigir(activo, "Registra y activa la ficha del buzo asignado.");
            jdbc.sql("""
                    INSERT INTO expedientes (id, version, operacion, contenido, buzo_id, publicado)
                    VALUES (:id, 1, :operacion, :contenido, :buzo, :publicado)
                    """).param("id", id).param("operacion", UUID.randomUUID())
                    .param("contenido", codificar(datos)).param("buzo", buzo).param("publicado", publicar).update();
        } else {
            verificarVersion(anterior, version);
            exigirEditable(anterior);
            actualizarDatos(id, datos);
            jdbc.sql("UPDATE expedientes SET publicado = :publicado WHERE id = :id")
                    .param("publicado", publicar).param("id", id).update();
        }
        return expediente(id, supervisor);
    }

    @Transactional
    public void eliminar(UUID id, String supervisor, long version) {
        faenas.bloquear(id, supervisor);
        var fila = fila(id);
        verificarVersion(fila, version);
        exigirEditable(fila);
        jdbc.sql("DELETE FROM expedientes WHERE id = :id").param("id", id).update();
    }

    @Transactional(noRollbackFor = Faenas.BloqueoRegistrado.class)
    public Expediente decidir(UUID id, String jefe, long version, SolicitudDecision solicitud) {
        faenas.bloquear(id, null);
        var fila = buscar(id);
        if (fila == null && solicitud.resultado() == Resultado.AUTORIZADA) {
            throw faenas.registrarBloqueo(id, "APROBACION", jefe, "Falta el expediente de prechequeo.");
        }
        if (fila == null) throw noEncontrado("Expediente no encontrado.");
        if (!fila.publicado()) {
            throw faenas.registrarBloqueo(id, "APROBACION", jefe, "El supervisor todavía está enviando el expediente.");
        }
        verificarVersion(fila, version);
        exigirEditable(fila);
        String motivo = solicitud.motivo() == null ? null : solicitud.motivo().strip();
        if (solicitud.resultado() == Resultado.RECHAZADA && (motivo == null || motivo.isEmpty())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica el motivo del rechazo.");
        }
        if (solicitud.resultado() == Resultado.AUTORIZADA) {
            var evaluacion = evaluar(faenas.consultar(id, null), fila.datos(), fotos(id));
            bloquearSiCorresponde(id, "APROBACION", jefe, evaluacion);
            if (evaluacion.nivel() == SemaforoFaena.Nivel.AMARILLO && (motivo == null || motivo.isEmpty())) {
                throw faenas.registrarBloqueo(id, "APROBACION", jefe,
                        "Justifica la revisión de las observaciones: " + String.join(" ", evaluacion.observaciones()));
            }
        }
        var decision = new Decision(solicitud.resultado(), motivo, jefe, ahora());
        jdbc.sql("UPDATE expedientes SET decision = :decision, version = version + 1 WHERE id = :id")
                .param("id", id).param("decision", codificar(decision)).update();
        return expediente(id, null);
    }

    @Transactional
    public Expediente iniciar(UUID id, String supervisor) {
        faenas.bloquear(id, supervisor);
        var fila = fila(id);
        exigir(fila.decision() != null && fila.decision().resultado() == Resultado.AUTORIZADA,
                "La faena requiere autorización de jefatura.");
        exigir(faenas.consultar(id, supervisor).cierre() == null, "La faena ya tiene un cierre.");
        if (fila.inicio() == null) {
            jdbc.sql("""
                    UPDATE expedientes SET iniciada_en = :fecha, iniciada_por = :supervisor, version = version + 1
                    WHERE id = :id
                    """).param("id", id).param("fecha", ahora()).param("supervisor", supervisor).update();
        }
        return expediente(id, supervisor);
    }

    @Transactional(noRollbackFor = Faenas.BloqueoRegistrado.class)
    public Bitacora cerrar(UUID id, String supervisor, Cierre cierre) {
        faenas.bloquear(id, supervisor);
        var fila = buscar(id);
        if (fila == null) {
            throw faenas.registrarBloqueo(id, "CIERRE", supervisor, "Falta el expediente de prechequeo.");
        }
        var apertura = faenas.consultar(id, supervisor);
        if (apertura.cierre() != null) {
            var existente = bitacora(apertura);
            exigir(existente != null && existente.mediciones().equals(cierre.mediciones())
                            && existente.duracionMinutos() == cierre.duracionMinutos()
                            && existente.observaciones().equals(cierre.observaciones())
                            && existente.saludSalida().equals(cierre.saludSalida()),
                    "Ya existe un cierre distinto para esta faena.");
            return existente;
        }
        bloquearSiCorresponde(id, "CIERRE", supervisor, evaluar(apertura, fila.datos(), fotos(id)));
        if (fila.inicio() == null || fila.decision() == null || fila.decision().resultado() != Resultado.AUTORIZADA) {
            throw faenas.registrarBloqueo(id, "CIERRE", supervisor, "La faena debe estar autorizada y en ejecución.");
        }
        exigir(cierre.mediciones().cerradoEn() >= fila.inicio().registradaEn().toInstant().toEpochMilli(),
                "La fecha de cierre debe ser posterior al inicio.");
        faenas.insertarCierre(id, cierre.mediciones());
        jdbc.sql("""
                INSERT INTO bitacoras (faena, duracion, observaciones, responsable, salud_salida)
                VALUES (:id, :duracion, :observaciones, :responsable, :salud)
                """).param("id", id).param("duracion", cierre.duracionMinutos())
                .param("observaciones", cierre.observaciones()).param("responsable", supervisor)
                .param("salud", codificar(cierre.saludSalida())).update();
        for (TipoAlerta tipo : TipoAlerta.values()) {
            if (velocidad(cierre.mediciones(), tipo) > limite(tipo)) {
                jdbc.sql("INSERT INTO alertas_inmersion (faena, tipo) VALUES (:id, :tipo)")
                        .param("id", id).param("tipo", tipo.name()).update();
            }
        }
        jdbc.sql("UPDATE expedientes SET version = version + 1 WHERE id = :id").param("id", id).update();
        return bitacora(faenas.consultar(id, supervisor));
    }

    @Transactional
    public Bitacora revisarAlerta(UUID id, TipoAlerta tipo, String jefe) {
        faenas.bloquear(id, null);
        boolean existe = jdbc.sql("SELECT COUNT(*) FROM alertas_inmersion WHERE faena = :id AND tipo = :tipo")
                .param("id", id).param("tipo", tipo.name()).query(Integer.class).single() > 0;
        if (!existe) throw noEncontrado("Alerta no encontrada.");
        int revisadas = jdbc.sql("""
                UPDATE alertas_inmersion SET revisada_en = :fecha, revisada_por = :jefe
                WHERE faena = :id AND tipo = :tipo AND revisada_en IS NULL
                """).param("id", id).param("tipo", tipo.name()).param("fecha", ahora()).param("jefe", jefe).update();
        if (revisadas == 1) {
            jdbc.sql("UPDATE expedientes SET version = version + 1 WHERE id = :id").param("id", id).update();
        }
        return bitacora(faenas.consultar(id, null));
    }

    Imagen imagen(UUID id, String supervisor, Equipo equipo, TipoFoto tipo) {
        faenas.consultar(id, supervisor);
        return jdbc.sql("""
                SELECT formato, tomada_en, responsable, imagen FROM evidencias_faena
                WHERE faena = :id AND equipo = :equipo AND tipo = :tipo
                """).param("id", id).param("equipo", equipo.name()).param("tipo", tipo.name())
                .query((rs, n) -> new Imagen(leerFoto(id, equipo, tipo, rs), rs.getBytes("imagen")))
                .optional().orElseThrow(() -> noEncontrado("Fotografía no encontrada."));
    }

    @Transactional
    public Foto guardarFoto(UUID id, String supervisor, long version, Equipo equipo, TipoFoto tipo,
                           String formato, byte[] bytes) {
        faenas.bloquear(id, supervisor);
        var fila = fila(id);
        verificarVersion(fila, version);
        exigirEditable(fila);
        var foto = new Foto(id, equipo, tipo, formato, ahora(), supervisor);
        jdbc.sql("""
                MERGE INTO evidencias_faena (faena, equipo, tipo, formato, tomada_en, responsable, imagen)
                KEY(faena, equipo, tipo) VALUES (:id, :equipo, :tipo, :formato, :fecha, :responsable, :imagen)
                """).param("id", id).param("equipo", equipo.name()).param("tipo", tipo.name())
                .param("formato", formato).param("fecha", foto.tomadaEn()).param("responsable", supervisor)
                .param("imagen", bytes).update();
        invalidarComparacion(id, fila.datos(), equipo);
        return foto;
    }

    @Transactional
    public void eliminarFoto(UUID id, String supervisor, long version, Equipo equipo, TipoFoto tipo) {
        faenas.bloquear(id, supervisor);
        var fila = fila(id);
        verificarVersion(fila, version);
        exigirEditable(fila);
        int eliminadas = jdbc.sql("DELETE FROM evidencias_faena WHERE faena = :id AND equipo = :equipo AND tipo = :tipo")
                .param("id", id).param("equipo", equipo.name()).param("tipo", tipo.name()).update();
        if (eliminadas == 0) throw noEncontrado("Fotografía no encontrada.");
        invalidarComparacion(id, fila.datos(), equipo);
    }

    private SemaforoFaena.Evaluacion evaluar(Faenas.Apertura apertura, ExpedienteDatos datos, List<Foto> fotos) {
        var capturas = EnumSet.noneOf(Equipo.class);
        fotos.stream().filter(foto -> foto.tipo() == TipoFoto.CAPTURA)
                .forEach(foto -> capturas.add(foto.equipo()));
        return semaforo.evaluar(apertura.datos(), datos, capturas);
    }

    private void bloquearSiCorresponde(UUID id, String operacion, String responsable, SemaforoFaena.Evaluacion evaluacion) {
        if (evaluacion.nivel() == SemaforoFaena.Nivel.ROJO) {
            throw faenas.registrarBloqueo(id, operacion, responsable, String.join(" ", evaluacion.bloqueos()));
        }
    }

    private Expediente expediente(UUID id, String supervisor) {
        var apertura = faenas.consultar(id, supervisor);
        var fila = fila(id);
        var fotos = fotos(id);
        return new Expediente(apertura, fila.version(), fila.datos(), fotos, fila.decision(), fila.inicio(),
                bitacora(apertura), evaluar(apertura, fila.datos(), fotos), fila.publicado());
    }

    private Bitacora bitacora(Faenas.Apertura apertura) {
        if (apertura.cierre() == null) return null;
        UUID id = apertura.id();
        var mediciones = apertura.cierre();
        var alertas = jdbc.sql("SELECT * FROM alertas_inmersion WHERE faena = :id ORDER BY tipo")
                .param("id", id).query((rs, n) -> {
                    var tipo = TipoAlerta.valueOf(rs.getString("tipo"));
                    return new Alerta(tipo, velocidad(mediciones, tipo), limite(tipo), mediciones.cerradoEn(),
                            apertura.supervisorACargo(), rs.getObject("revisada_en", OffsetDateTime.class),
                            rs.getString("revisada_por"));
                }).list();
        return jdbc.sql("SELECT * FROM bitacoras WHERE faena = :id").param("id", id)
                .query((rs, n) -> new Bitacora(id, apertura.datos().buzoId(), rs.getInt("duracion"),
                        rs.getString("observaciones"), rs.getString("responsable"),
                        decodificar(rs.getString("salud_salida"), Salud.class), mediciones, alertas))
                .optional().orElse(null);
    }

    private List<Foto> fotos(UUID id) {
        return jdbc.sql("""
                SELECT equipo, tipo, formato, tomada_en, responsable FROM evidencias_faena
                WHERE faena = :id ORDER BY equipo, tipo
                """).param("id", id).query((rs, n) -> leerFoto(id, Equipo.valueOf(rs.getString("equipo")),
                        TipoFoto.valueOf(rs.getString("tipo")), rs)).list();
    }

    private static Foto leerFoto(UUID id, Equipo equipo, TipoFoto tipo, ResultSet rs) throws SQLException {
        return new Foto(id, equipo, tipo, rs.getString("formato"), rs.getObject("tomada_en", OffsetDateTime.class),
                rs.getString("responsable"));
    }

    private Fila buscar(UUID id) {
        return jdbc.sql("SELECT * FROM expedientes WHERE id = :id").param("id", id).query((rs, n) -> {
            var iniciada = rs.getObject("iniciada_en", OffsetDateTime.class);
            return new Fila(rs.getLong("version"), decodificar(rs.getString("contenido"), ExpedienteDatos.class),
                    decodificar(rs.getString("decision"), Decision.class),
                    iniciada == null ? null : new Inicio(iniciada, rs.getString("iniciada_por")), rs.getBoolean("publicado"));
        }).optional().orElse(null);
    }

    private Fila fila(UUID id) {
        var fila = buscar(id);
        if (fila == null) throw noEncontrado("Expediente no encontrado.");
        return fila;
    }

    private void actualizarDatos(UUID id, ExpedienteDatos datos) {
        jdbc.sql("UPDATE expedientes SET contenido = :contenido, version = version + 1 WHERE id = :id")
                .param("id", id).param("contenido", codificar(datos)).update();
    }

    private void invalidarComparacion(UUID id, ExpedienteDatos datos, Equipo equipo) {
        var checklist = datos.checklist().stream()
                .map(i -> i.equipo() == equipo ? new Inspeccion(i.equipo(), i.estado(), i.notas(), null) : i)
                .toList();
        actualizarDatos(id, new ExpedienteDatos(checklist, datos.saludInicial(), datos.ast(), datos.tareas()));
    }

    private String codificar(Object datos) {
        try {
            return json.writeValueAsString(datos);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo guardar el expediente.", e);
        }
    }

    private <T> T decodificar(String datos, Class<T> tipo) {
        if (datos == null) return null;
        try {
            return json.readValue(datos, tipo);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo leer el expediente.", e);
        }
    }

    private static boolean tieneFoto(List<Foto> fotos, Equipo equipo, TipoFoto tipo) {
        return fotos.stream().anyMatch(f -> f.equipo() == equipo && f.tipo() == tipo);
    }

    private static void verificarVersion(Fila fila, long version) {
        exigir(fila.version() == version, "El expediente cambió. Consulta la versión actual.");
    }

    private static void exigirEditable(Fila fila) {
        exigir(fila.decision() == null, "El expediente ya tiene una decisión de jefatura.");
    }

    private static double limite(TipoAlerta tipo) {
        return tipo == TipoAlerta.DESCENSO ? 24 : 9;
    }

    private static double velocidad(CierreHistorico mediciones, TipoAlerta tipo) {
        return tipo == TipoAlerta.DESCENSO ? mediciones.descensoMaximo() : mediciones.ascensoMaximo();
    }

    private static OffsetDateTime ahora() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    private static void exigir(boolean condicion, String mensaje) {
        if (!condicion) throw new ResponseStatusException(HttpStatus.CONFLICT, mensaje);
    }

    private static ResponseStatusException noEncontrado(String mensaje) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, mensaje);
    }
}
