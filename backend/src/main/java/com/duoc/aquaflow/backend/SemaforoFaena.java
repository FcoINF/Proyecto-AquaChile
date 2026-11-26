package com.duoc.aquaflow.backend;

import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import static com.duoc.aquaflow.backend.ExpedienteDatos.*;

@Component
public class SemaforoFaena {
    public enum Nivel { VERDE, AMARILLO, ROJO }
    public record Evaluacion(Nivel nivel, List<String> bloqueos, List<String> observaciones) {}

    private final Validator validator;
    private final double imcMin;
    private final double imcMax;
    private final int calbucoMin;

    SemaforoFaena(Validator validator,
                  @Value("${aquaflow.academico.imc-min:18.5}") double imcMin,
                  @Value("${aquaflow.academico.imc-max:25.0}") double imcMax,
                  @Value("${aquaflow.academico.calbuco-min:80}") int calbucoMin) {
        if (!Double.isFinite(imcMin) || !Double.isFinite(imcMax) || imcMin < 10 || imcMax > 60
                || imcMin >= imcMax || calbucoMin < 0 || calbucoMin > 100) {
            throw new IllegalArgumentException("Revisa los umbrales académicos de IMC y Calbuco.");
        }
        this.validator = validator;
        this.imcMin = imcMin;
        this.imcMax = imcMax;
        this.calbucoMin = calbucoMin;
    }

    Evaluacion evaluar(AperturaDatos apertura, ExpedienteDatos datos, Set<Equipo> capturas) {
        var bloqueos = new ArrayList<String>();
        var observaciones = new ArrayList<String>();
        var erroresApertura = validator.validate(apertura).stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage()).sorted().toList();
        bloqueos.addAll(erroresApertura);
        if (erroresApertura.isEmpty()) {
            try {
                apertura.validarVigencias();
            } catch (ResponseStatusException e) {
                bloqueos.add(e.getReason());
            }
            if (apertura.imcBuzo() < imcMin || apertura.imcBuzo() >= imcMax) {
                observaciones.add("IMC fuera del rango académico de referencia.");
            }
            if (apertura.puntajeCalbuco() < calbucoMin) {
                observaciones.add("Puntaje Calbuco inferior al mínimo académico de referencia.");
            }
        }

        var inspeccionados = EnumSet.noneOf(Equipo.class);
        for (var item : datos.checklist()) {
            if (item.estado() != EstadoEquipo.PENDIENTE) inspeccionados.add(item.equipo());
            if (item.estado() == EstadoEquipo.NO_CUMPLE) bloqueos.add("Equipo en No cumple: " + item.equipo() + ".");
            if (item.estado() == EstadoEquipo.OBSERVADO) observaciones.add("Equipo observado: " + item.equipo() + ".");
            if (Boolean.FALSE.equals(item.coincideConReferencia())) {
                observaciones.add("La captura difiere de la referencia: " + item.equipo() + ".");
            }
        }
        for (Equipo equipo : Equipo.values()) {
            if (!inspeccionados.contains(equipo)) bloqueos.add("Falta inspección de " + equipo + ".");
            if (!capturas.contains(equipo)) bloqueos.add("Falta foto obligatoria de " + equipo + ".");
        }

        var salud = datos.saludInicial();
        if (salud == null) {
            bloqueos.add("Falta la encuesta de salud inicial.");
        } else if (salud.saturacion() < 92 || salud.sistolica() >= 160 || salud.diastolica() >= 100) {
            bloqueos.add("La encuesta de salud inicial contiene valores críticos.");
        } else if (salud.saturacion() < 95 || salud.sistolica() >= 140 || salud.diastolica() >= 90
                || salud.respuestas().contains(false)) {
            observaciones.add("La encuesta de salud inicial requiere revisión.");
        }

        var ast = datos.ast();
        if (ast == null) {
            bloqueos.add("Falta el AST.");
        } else if (ast.respuestas().subList(0, 3).contains(false)) {
            bloqueos.add("El AST registra una condición crítica.");
        } else if (ast.requiereNuevoAst() || ast.respuestas().contains(false)) {
            observaciones.add("El AST requiere revisión.");
        }
        if (datos.tareas().isEmpty() || datos.tareas().stream().anyMatch(tarea -> !tarea.verificada())) {
            bloqueos.add("Completa la lista de tareas y verifica todos sus ítems.");
        }

        Nivel nivel = !bloqueos.isEmpty() ? Nivel.ROJO : observaciones.isEmpty() ? Nivel.VERDE : Nivel.AMARILLO;
        return new Evaluacion(nivel, List.copyOf(bloqueos), List.copyOf(observaciones));
    }
}
