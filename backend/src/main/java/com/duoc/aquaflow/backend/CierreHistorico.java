package com.duoc.aquaflow.backend;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CierreHistorico(
        @Positive long cerradoEn,
        @Positive int tiempoFondoMinutos,
        @Positive double profundidadMaximaMetros,
        @Positive double descensoMaximo,
        @Positive double ascensoMaximo,
        @NotNull Origen origen
) {
    public enum Origen { MANUAL, CENSUS }

    @JsonIgnore
    @AssertTrue(message = "Las mediciones deben ser valores finitos")
    public boolean isMedicionesValidas() {
        return Double.isFinite(profundidadMaximaMetros)
                && Double.isFinite(descensoMaximo) && Double.isFinite(ascensoMaximo);
    }
}
