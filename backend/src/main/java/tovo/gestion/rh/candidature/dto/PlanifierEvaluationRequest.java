package tovo.gestion.rh.candidature.dto;

import jakarta.validation.constraints.NotNull;
import tovo.gestion.rh.candidature.entity.TypeEvaluation;

import java.time.LocalDateTime;

public record PlanifierEvaluationRequest(
        @NotNull TypeEvaluation type,
        @NotNull LocalDateTime date,
        @NotNull Integer evaluateurId
) {}
