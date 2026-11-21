package com.duoc.aquaflow.backend;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@SecurityRequirement(name = "cuenta")
class FaenasController {
    public record Sesion(String nombre, String rol) {}

    private final Faenas faenas;

    FaenasController(Faenas faenas) {
        this.faenas = faenas;
    }

    @GetMapping("/sesion")
    Sesion sesion(Authentication cuenta) {
        return new Sesion(cuenta.getName(), jefatura(cuenta) ? "JEFE" : "SUPERVISOR");
    }

    @Operation(summary = "Listar aperturas confirmadas visibles para la cuenta")
    @GetMapping("/faenas")
    List<Faenas.Apertura> listar(Authentication cuenta) {
        return faenas.listar(supervisorVisible(cuenta));
    }

    @GetMapping("/faenas/{id}")
    Faenas.Apertura consultar(@PathVariable UUID id, Authentication cuenta) {
        return faenas.consultar(id, supervisorVisible(cuenta));
    }

    @Operation(summary = "Crear una apertura confirmada como supervisor")
    @PostMapping(value = "/faenas", consumes = "application/json")
    ResponseEntity<Faenas.Apertura> crear(@Valid @RequestBody AperturaDatos datos, Authentication cuenta) {
        var guardado = faenas.guardar(UUID.randomUUID(), cuenta.getName(), datos);
        return respuesta(guardado);
    }

    @Operation(summary = "Crear o modificar una apertura propia con un ID estable")
    @PutMapping(value = "/faenas/{id}", consumes = "application/json")
    ResponseEntity<Faenas.Apertura> guardar(
            @PathVariable UUID id, @Valid @RequestBody AperturaDatos datos, Authentication cuenta) {
        return respuesta(faenas.guardar(id, cuenta.getName(), datos));
    }

    @DeleteMapping("/faenas/{id}")
    ResponseEntity<Void> eliminar(@PathVariable UUID id, Authentication cuenta) {
        faenas.eliminar(id, cuenta.getName());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Publicar las mediciones históricas de una faena propia")
    @PutMapping(value = "/faenas/{id}/cierre-historico", consumes = "application/json")
    ResponseEntity<Void> publicarCierre(
            @PathVariable UUID id, @Valid @RequestBody CierreHistorico cierre, Authentication cuenta) {
        faenas.publicarCierre(id, cuenta.getName(), cierre);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail datosInvalidos(MethodArgumentNotValidException error) {
        var campos = error.getBindingResult().getFieldErrors().stream()
                .map(campo -> campo.getField() + " (" + campo.getDefaultMessage() + ")")
                .sorted().toList();
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Revisa los datos de la apertura. " + String.join(", ", campos));
    }

    private static ResponseEntity<Faenas.Apertura> respuesta(Faenas.Guardado guardado) {
        var apertura = guardado.apertura();
        return guardado.creada()
                ? ResponseEntity.created(URI.create("/api/faenas/" + apertura.id())).body(apertura)
                : ResponseEntity.ok(apertura);
    }

    private static String supervisorVisible(Authentication cuenta) {
        return jefatura(cuenta) ? null : cuenta.getName();
    }

    private static boolean jefatura(Authentication cuenta) {
        return cuenta.getAuthorities().stream().anyMatch(rol -> rol.getAuthority().equals("ROLE_JEFE"));
    }
}
