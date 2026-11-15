package tovo.gestion.rh.temps.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;

public record ModifierPlanningRequest(@NotNull Long employeId, @NotNull LocalDate date,
        @NotNull LocalTime heureDebut, @NotNull LocalTime heureFin,
        @NotBlank String type, @NotBlank @Size(max = 20) String motif) {}
