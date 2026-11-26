package com.duoc.aquaflow.backend;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public record AperturaDatos(
        @NotBlank @Size(max = 120) String centroCultivo,
        @NotBlank @Size(max = 80) String centroId,
        @NotBlank @Size(max = 120) String buzoAsignado,
        @NotBlank @Size(max = 80) String buzoId,
        @NotBlank @Pattern(regexp = "[0-9]{2}/[0-9]{2}/[0-9]{4}") String fecha,
        @NotBlank @Pattern(regexp = "([01][0-9]|2[0-3]):[0-5][0-9]") String hora,
        @NotNull @Min(18) @Max(80) Integer edadBuzo,
        @NotNull @DecimalMin("10.0") @DecimalMax("60.0") Double imcBuzo,
        @NotNull @Min(0) @Max(100) Integer puntajeCalbuco,
        @NotNull @AssertTrue(message = "El examen preocupacional debe estar vigente.") Boolean examenPreocupacionalVigente,
        @NotBlank @Size(max = 10) String vigenciaExamenPreocupacional,
        Boolean testEsfuerzoVigente,
        @Size(max = 10) String vigenciaTestEsfuerzo,
        @NotNull @AssertTrue(message = "El buzo de emergencia debe estar disponible.") Boolean buzoEmergenciaDisponible,
        @NotNull @AssertTrue(message = "Confirma la verificación meteorológica.") Boolean condicionesListas,
        @NotNull @AssertTrue(message = "Confirma la disponibilidad de la embarcación.") Boolean embarcacionLista,
        @NotNull @AssertTrue(message = "Confirma que las comunicaciones están operativas.") Boolean comunicacionesOperativas,
        CondicionesMaritimas meteorologia) {

    public record CondicionesMaritimas(double latitudModelo, double longitudModelo, long consultadaEn,
            long horaModelo, Double alturaOla, Double periodoOla, Double direccionOla,
            Double temperaturaSuperficial) {}

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT);

    void validarVigencias() {
        LocalDate dia = fechaOError(fecha, "Ingresa una fecha válida para la faena.");
        LocalDate examen = fechaOError(vigenciaExamenPreocupacional, "Ingresa una vigencia válida para el examen.");
        if (examen.isBefore(dia)) {
            throw bloqueo("El examen preocupacional debe estar vigente a la fecha de la faena.");
        }
        if (edadBuzo > 45 && !Boolean.TRUE.equals(testEsfuerzoVigente)) {
            throw bloqueo("Los buzos mayores de 45 años requieren un test de esfuerzo vigente.");
        }
        if (Boolean.TRUE.equals(testEsfuerzoVigente)) {
            LocalDate test = fechaOError(vigenciaTestEsfuerzo, "Ingresa una vigencia válida para el test de esfuerzo.");
            if (test.isBefore(dia)) {
                throw bloqueo("El test de esfuerzo debe estar vigente a la fecha de la faena.");
            }
        }
    }

    private static LocalDate fechaOError(String valor, String mensaje) {
        if (valor == null) {
            throw bloqueo(mensaje);
        }
        try {
            LocalDate dia = LocalDate.parse(valor, FECHA);
            if (dia.getYear() < 1) {
                throw bloqueo(mensaje);
            }
            return dia;
        } catch (DateTimeParseException e) {
            throw bloqueo(mensaje);
        }
    }

    private static ResponseStatusException bloqueo(String mensaje) {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, mensaje);
    }
}
