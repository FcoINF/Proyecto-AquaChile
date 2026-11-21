package com.duoc.aquaflow.backend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
class Faenas {
    public record Apertura(UUID id, String supervisorACargo, OffsetDateTime actualizadaEn,
                           AperturaDatos datos, CierreHistorico cierre) {}

    private static final String CONSULTA_APERTURAS = """
            SELECT a.*, c.cerrado_en, c.tiempo_fondo, c.profundidad, c.descenso, c.ascenso, c.origen
            FROM aperturas a LEFT JOIN cierres_historicos c ON a.id = c.id
            """;

    record Guardado(Apertura apertura, boolean creada) {}

    private final JdbcClient jdbc;
    private final ObjectMapper json;

    Faenas(JdbcClient jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    List<Apertura> listar(String supervisor) {
        String consulta = CONSULTA_APERTURAS;
        String orden = " ORDER BY a.actualizada_en DESC, a.id";
        if (supervisor == null) {
            return jdbc.sql(consulta + orden).query(this::leer).list();
        }
        return jdbc.sql(consulta + " WHERE supervisor = :supervisor" + orden)
                .param("supervisor", supervisor).query(this::leer).list();
    }

    Apertura consultar(UUID id, String supervisor) {
        String consulta = CONSULTA_APERTURAS + " WHERE a.id = :id";
        var fila = supervisor == null
                ? jdbc.sql(consulta).param("id", id)
                : jdbc.sql(consulta + " AND supervisor = :supervisor").param("id", id)
                        .param("supervisor", supervisor);
        return fila.query(this::leer).optional().orElseThrow(Faenas::noEncontrada);
    }

    Guardado guardar(UUID id, String supervisor, AperturaDatos datos) {
        datos.validarVigencias();
        String contenido;
        try {
            contenido = json.writeValueAsString(datos);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo codificar la apertura.", e);
        }
        var ahora = OffsetDateTime.now(ZoneOffset.UTC);
        var apertura = new Apertura(id, supervisor, ahora, datos, null);
        int actualizadas = jdbc.sql("""
                UPDATE aperturas SET datos = :datos, actualizada_en = :ahora
                WHERE id = :id AND supervisor = :supervisor
                AND NOT EXISTS (SELECT 1 FROM cierres_historicos WHERE id = :id)
                AND NOT EXISTS (SELECT 1 FROM expedientes WHERE id = :id)
                """)
                .param("datos", contenido).param("ahora", ahora)
                .param("id", id).param("supervisor", supervisor).update();
        if (actualizadas == 1) {
            return new Guardado(apertura, false);
        }
        try {
            jdbc.sql("""
                    INSERT INTO aperturas (id, supervisor, datos, actualizada_en)
                    VALUES (:id, :supervisor, :datos, :ahora)
                    """).param("id", id).param("supervisor", supervisor)
                    .param("datos", contenido).param("ahora", ahora).update();
            return new Guardado(apertura, true);
        } catch (DuplicateKeyException e) {
            consultar(id, supervisor);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La apertura cambió. Reintenta el guardado.");
        }
    }

    void eliminar(UUID id, String supervisor) {
        int eliminadas = jdbc.sql("""
                DELETE FROM aperturas WHERE id = :id AND supervisor = :supervisor
                AND NOT EXISTS (SELECT 1 FROM expedientes WHERE id = :id)
                AND NOT EXISTS (SELECT 1 FROM cierres_historicos WHERE id = :id)
                """)
                .param("id", id).param("supervisor", supervisor).update();
        if (eliminadas == 0) {
            if (consultar(id, supervisor).cierre() != null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "La apertura tiene un cierre histórico.");
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La apertura tiene un expediente sincronizado.");
        }
    }

    @Transactional
    public void publicarCierre(UUID id, String supervisor, CierreHistorico cierre) {
        bloquear(id, supervisor);
        var previo = consultar(id, supervisor).cierre();
        if (cierre.equals(previo)) return;
        if (jdbc.sql("SELECT COUNT(*) FROM expedientes WHERE id = :id").param("id", id).query(Integer.class).single() > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Usa la bitácora para cerrar un expediente completo.");
        }
        if (previo != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un cierre histórico para esta faena.");
        }
        insertarCierre(id, cierre);
    }

    void bloquear(UUID id, String supervisor) {
        var seleccion = jdbc.sql("SELECT id FROM aperturas WHERE id = :id"
                + (supervisor == null ? "" : " AND supervisor = :supervisor") + " FOR UPDATE").param("id", id);
        if (supervisor != null) seleccion.param("supervisor", supervisor);
        seleccion.query(UUID.class).optional().orElseThrow(Faenas::noEncontrada);
    }

    void insertarCierre(UUID id, CierreHistorico cierre) {
        jdbc.sql("""
                INSERT INTO cierres_historicos (id, cerrado_en, tiempo_fondo, profundidad, descenso, ascenso, origen)
                VALUES (:id, :fecha, :fondo, :profundidad, :descenso, :ascenso, :origen)
                """).param("id", id).param("fecha", cierre.cerradoEn()).param("fondo", cierre.tiempoFondoMinutos())
                .param("profundidad", cierre.profundidadMaximaMetros()).param("descenso", cierre.descensoMaximo())
                .param("ascenso", cierre.ascensoMaximo()).param("origen", cierre.origen().name()).update();
    }

    private Apertura leer(ResultSet fila, int numero) throws SQLException {
        try {
            var origen = fila.getString("origen");
            var cierre = origen == null ? null : new CierreHistorico(
                    fila.getLong("cerrado_en"), fila.getInt("tiempo_fondo"), fila.getDouble("profundidad"),
                    fila.getDouble("descenso"), fila.getDouble("ascenso"), CierreHistorico.Origen.valueOf(origen));
            return new Apertura(fila.getObject("id", UUID.class), fila.getString("supervisor"),
                    fila.getObject("actualizada_en", OffsetDateTime.class),
                    json.readValue(fila.getString("datos"), AperturaDatos.class), cierre);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("La apertura persistida no se pudo leer.", e);
        }
    }

    private static ResponseStatusException noEncontrada() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Apertura no encontrada.");
    }
}
