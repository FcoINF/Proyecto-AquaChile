package com.duoc.aquaflow.backend;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/buzos")
@SecurityRequirement(name = "cuenta")
class BuzosController {
    public record Ficha(String id, String nombre, boolean activo) {}
    public record Datos(@NotBlank @Size(max = 120) String nombre, @NotNull Boolean activo) {}

    private final JdbcClient jdbc;

    BuzosController(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    @Operation(summary = "Listar fichas de buzos ficticios")
    List<Ficha> listar() {
        return jdbc.sql("SELECT id, nombre, activo FROM buzos ORDER BY nombre, id").query(Ficha.class).list();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar la ficha de un buzo")
    Ficha consultar(@PathVariable String id) {
        return jdbc.sql("SELECT id, nombre, activo FROM buzos WHERE id = :id")
                .param("id", id).query(Ficha.class).optional()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Buzo no encontrado."));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Jefatura: crear, actualizar o desactivar una ficha")
    Ficha guardar(@PathVariable @Pattern(regexp = "[A-Za-z0-9_-]{1,80}") String id,
                  @Valid @RequestBody Datos datos) {
        jdbc.sql("MERGE INTO buzos (id, nombre, activo) KEY(id) VALUES (:id, :nombre, :activo)")
                .param("id", id).param("nombre", datos.nombre().strip()).param("activo", datos.activo()).update();
        return new Ficha(id, datos.nombre().strip(), datos.activo());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Jefatura: eliminar una ficha sin expedientes vinculados")
    void eliminar(@PathVariable String id) {
        try {
            if (jdbc.sql("DELETE FROM buzos WHERE id = :id").param("id", id).update() == 0) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Buzo no encontrado.");
            }
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La ficha tiene expedientes. Puedes desactivarla.");
        }
    }
}
