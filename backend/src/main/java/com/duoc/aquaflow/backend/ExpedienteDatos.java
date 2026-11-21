package com.duoc.aquaflow.backend;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;

public record ExpedienteDatos(
        @NotNull @Size(max = 4) List<@NotNull @Valid Inspeccion> checklist,
        @Valid Salud saludInicial,
        @Valid Ast ast,
        @NotNull @Size(max = 50) List<@NotNull @Valid Tarea> tareas) {

    public enum Equipo { MALETA_AIRE, FILTROS_COMPRESOR, ALETAS, CENSUS }
    public enum EstadoEquipo { CUMPLE, OBSERVADO, NO_CUMPLE, PENDIENTE }
    public enum TipoFoto { CAPTURA, REFERENCIA }
    public enum Resultado { AUTORIZADA, RECHAZADA }
    public enum TipoAlerta { DESCENSO, ASCENSO }

    public record Inspeccion(@NotNull Equipo equipo, @NotNull EstadoEquipo estado,
                             @Size(max = 1000) String notas, Boolean coincideConReferencia) {}

    public record Salud(@NotNull @Min(70) @Max(250) Integer sistolica,
                        @NotNull @Min(40) @Max(150) Integer diastolica,
                        @NotNull @Min(50) @Max(100) Integer saturacion,
                        @NotNull @Size(min = 3, max = 3) List<@NotNull Boolean> respuestas) {}

    public record Ast(@NotBlank @Size(max = 120) String tipologia, boolean requiereNuevoAst,
                      @NotNull @Size(min = 5, max = 5) List<@NotNull Boolean> respuestas,
                      @NotNull @Size(max = 50) List<@NotNull @Valid Paso> pasos) {}

    public record Paso(@NotBlank @Size(max = 250) String tarea, @NotBlank @Size(max = 250) String peligro,
                       @NotBlank @Size(max = 250) String control, boolean critico) {}

    public record Tarea(@NotBlank @Size(max = 250) String descripcion, boolean verificada) {}

    public record SolicitudDecision(@NotNull Resultado resultado, @Size(max = 1000) String motivo) {}

    public record Decision(Resultado resultado, String motivo, String responsable, OffsetDateTime registradaEn) {}

    public record Cierre(@NotNull @Positive Integer duracionMinutos,
                         @NotBlank @Size(max = 1000) String observaciones,
                         @NotNull @AssertTrue Boolean revisionSalida,
                         @NotNull @Valid Salud saludSalida,
                         @NotNull @Valid CierreHistorico mediciones) {
        @JsonIgnore
        @AssertTrue(message = "El tiempo de fondo no puede superar la duración de la inmersión.")
        public boolean isTiempoDeFondoValido() {
            return mediciones == null || duracionMinutos == null || mediciones.tiempoFondoMinutos() <= duracionMinutos;
        }
    }
}
