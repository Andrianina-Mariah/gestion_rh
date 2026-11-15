package tovo.gestion.rh.temps.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record AssignationRegimeRequest(@NotNull Long employeId, @NotNull Long regimeTravailId,
        @NotNull LocalDate dateDebut, LocalDate dateFin) {}
