package com.duoc.aquaflow.backend;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static com.duoc.aquaflow.backend.ExpedienteDatos.*;

@RestController
@RequestMapping("/api/faenas/{id}")
@SecurityRequirement(name = "cuenta")
class ExpedientesController {
    private final Expedientes expedientes;

    ExpedientesController(Expedientes expedientes) {
        this.expedientes = expedientes;
    }

    @GetMapping("/expediente")
    @Operation(summary = "Consultar prechequeo, evidencias, decisión, inicio y bitácora")
    Expedientes.Expediente consultar(@PathVariable UUID id, Authentication cuenta) {
        return expedientes.consultar(id, supervisorVisible(cuenta));
    }

    @PutMapping("/expediente")
    @Operation(summary = "Supervisor propietario: crear o actualizar el prechequeo",
            description = "Usa version=0 para crearlo. Para editar, envía la versión de GET /expediente.")
    ResponseEntity<Expedientes.Expediente> guardar(@PathVariable UUID id, @RequestParam @Min(0) long version,
                                                  @RequestParam(defaultValue = "true") boolean publicar,
                                                  @Valid @RequestBody ExpedienteDatos datos, Authentication cuenta) {
        var guardado = expedientes.guardar(id, cuenta.getName(), version, datos, publicar);
        return ResponseEntity.status(version == 0 ? HttpStatus.CREATED : HttpStatus.OK).body(guardado);
    }

    @DeleteMapping("/expediente")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supervisor propietario: eliminar un expediente sin decisión y sus fotos")
    void eliminar(@PathVariable UUID id, @RequestParam @Min(1) long version, Authentication cuenta) {
        expedientes.eliminar(id, cuenta.getName(), version);
    }

    @PostMapping("/decision")
    @Operation(summary = "Jefatura: resolver el expediente con la versión revisada")
    Expedientes.Expediente decidir(@PathVariable UUID id, @RequestParam @Min(1) long version,
                                   @Valid @RequestBody SolicitudDecision datos, Authentication cuenta) {
        return expedientes.decidir(id, cuenta.getName(), version, datos);
    }

    @PutMapping("/inicio")
    @Operation(summary = "Supervisor propietario: iniciar una faena autorizada")
    Expedientes.Expediente iniciar(@PathVariable UUID id, Authentication cuenta) {
        return expedientes.iniciar(id, cuenta.getName());
    }

    @GetMapping("/bitacora")
    @Operation(summary = "Consultar bitácora de cierre y estado de revisión de alertas")
    Expedientes.Bitacora bitacora(@PathVariable UUID id, Authentication cuenta) {
        var bitacora = consultar(id, cuenta).bitacora();
        if (bitacora == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Bitácora no encontrada.");
        return bitacora;
    }

    @PutMapping("/bitacora")
    @Operation(summary = "Supervisor propietario: registrar el cierre y generar alertas de velocidad")
    Expedientes.Bitacora cerrar(@PathVariable UUID id, @Valid @RequestBody Cierre datos, Authentication cuenta) {
        return expedientes.cerrar(id, cuenta.getName(), datos);
    }

    @PutMapping("/bitacora/alertas/{tipo}/revision")
    @Operation(summary = "Jefatura: marcar una alerta como revisada")
    Expedientes.Bitacora revisar(@PathVariable UUID id, @PathVariable TipoAlerta tipo, Authentication cuenta) {
        return expedientes.revisarAlerta(id, tipo, cuenta.getName());
    }

    @GetMapping(value = "/evidencias/{equipo}/{tipo}", produces = { "image/jpeg", "image/png" })
    @Operation(summary = "Descargar la captura o foto de referencia de un equipo")
    ResponseEntity<byte[]> imagen(@PathVariable UUID id, @PathVariable Equipo equipo, @PathVariable TipoFoto tipo,
                                  Authentication cuenta) {
        var imagen = expedientes.imagen(id, supervisorVisible(cuenta), equipo, tipo);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(imagen.foto().formato())).body(imagen.bytes());
    }

    @PutMapping(value = "/evidencias/{equipo}/{tipo}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Supervisor propietario: cargar o sustituir una evidencia JPEG o PNG",
            description = "La carga incrementa la versión del expediente y anula la comparación anterior del equipo.")
    Expedientes.Foto subir(@PathVariable UUID id, @PathVariable Equipo equipo, @PathVariable TipoFoto tipo,
                           @RequestParam @Min(1) long version, @RequestPart MultipartFile archivo,
                           Authentication cuenta) throws IOException {
        byte[] bytes = archivo.getBytes();
        String formato = formatoImagen(bytes);
        return expedientes.guardarFoto(id, cuenta.getName(), version, equipo, tipo, formato, bytes);
    }

    @DeleteMapping("/evidencias/{equipo}/{tipo}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supervisor propietario: retirar una evidencia de un expediente sin decisión")
    void eliminarFoto(@PathVariable UUID id, @PathVariable Equipo equipo, @PathVariable TipoFoto tipo,
                      @RequestParam @Min(1) long version, Authentication cuenta) {
        expedientes.eliminarFoto(id, cuenta.getName(), version, equipo, tipo);
    }

    private static String formatoImagen(byte[] bytes) {
        try (var entrada = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            var lectores = ImageIO.getImageReaders(entrada);
            if (!lectores.hasNext()) throw imagenInvalida();
            var lector = lectores.next();
            try {
                String formato = switch (lector.getFormatName().toLowerCase(Locale.ROOT)) {
                    case "jpeg" -> MediaType.IMAGE_JPEG_VALUE;
                    case "png" -> MediaType.IMAGE_PNG_VALUE;
                    default -> throw imagenInvalida();
                };
                lector.setInput(entrada, true, true);
                if ((long) lector.getWidth(0) * lector.getHeight(0) > 24_000_000) {
                    throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                            "La imagen supera los 24 megapíxeles.");
                }
                if (lector.read(0) == null) throw imagenInvalida();
                return formato;
            } finally {
                lector.dispose();
            }
        } catch (IOException | IllegalArgumentException e) {
            throw imagenInvalida();
        }
    }

    private static ResponseStatusException imagenInvalida() {
        return new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Adjunta una fotografía JPEG o PNG legible.");
    }

    private static String supervisorVisible(Authentication cuenta) {
        return cuenta.getAuthorities().stream().anyMatch(rol -> rol.getAuthority().equals("ROLE_JEFE"))
                ? null : cuenta.getName();
    }
}
