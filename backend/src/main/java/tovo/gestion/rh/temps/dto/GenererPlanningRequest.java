package tovo.gestion.rh.temps.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record GenererPlanningRequest(Long employeId, Integer departementId,
        @NotNull LocalDate du, @NotNull LocalDate au) {}
