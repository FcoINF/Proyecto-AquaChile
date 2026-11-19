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
import org.springframework.web.server.ResponseStatusException;

@Service
class Faenas {
    public record Apertura(UUID id, String supervisorACargo, OffsetDateTime actualizadaEn, AperturaDatos datos) {}

    record Guardado(Apertura apertura, boolean creada) {}

    private final JdbcClient jdbc;
    private final ObjectMapper json;

    Faenas(JdbcClient jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    List<Apertura> listar(String supervisor) {
        String consulta = "SELECT * FROM aperturas";
        String orden = " ORDER BY actualizada_en DESC, id";
        if (supervisor == null) {
            return jdbc.sql(consulta + orden).query(this::leer).list();
        }
        return jdbc.sql(consulta + " WHERE supervisor = :supervisor" + orden)
                .param("supervisor", supervisor).query(this::leer).list();
    }

    Apertura consultar(UUID id, String supervisor) {
        String consulta = "SELECT * FROM aperturas WHERE id = :id";
        var fila = supervisor == null
                ? jdbc.sql(consulta).param("id", id)
                : jdbc.sql(consulta + " AND supervisor = :supervisor").param("id", id)
                        .param("supervisor", supervisor);
        return fila.query(this::leer).optional().orElseThrow(Faenas::noEncontrada);
    }

    Guardado guardar(UUID id, String supervisor, AperturaDatos datos) {
        return guardar(id, supervisor, datos, false);
    }

    Guardado guardarExpediente(UUID id, String supervisor, AperturaDatos datos) {
        return guardar(id, supervisor, datos, true);
    }

    private Guardado guardar(UUID id, String supervisor, AperturaDatos datos, boolean sincronizacion) {
        datos.validarVigencias();
        String contenido;
        try {
            contenido = json.writeValueAsString(datos);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo codificar la apertura.", e);
        }
        var ahora = OffsetDateTime.now(ZoneOffset.UTC);
        var apertura = new Apertura(id, supervisor, ahora, datos);
        int actualizadas = jdbc.sql("""
                UPDATE aperturas SET datos = :datos, actualizada_en = :ahora
                WHERE id = :id AND supervisor = :supervisor
                """ + (sincronizacion ? "" : " AND NOT EXISTS (SELECT 1 FROM expedientes WHERE id = :id)"))
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
                """)
                .param("id", id).param("supervisor", supervisor).update();
        if (eliminadas == 0) {
            consultar(id, supervisor);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La apertura tiene un expediente sincronizado.");
        }
    }

    private Apertura leer(ResultSet fila, int numero) throws SQLException {
        try {
            return new Apertura(fila.getObject("id", UUID.class), fila.getString("supervisor"),
                    fila.getObject("actualizada_en", OffsetDateTime.class),
                    json.readValue(fila.getString("datos"), AperturaDatos.class));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("La apertura persistida no se pudo leer.", e);
        }
    }

    private static ResponseStatusException noEncontrada() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Apertura no encontrada.");
    }
}
