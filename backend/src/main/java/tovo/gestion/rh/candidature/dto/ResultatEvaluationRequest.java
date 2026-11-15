package tovo.gestion.rh.candidature.dto;

import jakarta.validation.constraints.NotNull;
import tovo.gestion.rh.candidature.entity.ResultatEvaluation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ResultatEvaluationRequest(
        @NotNull LocalDateTime date,
        BigDecimal note,
        String remarque,
        @NotNull ResultatEvaluation resultat
) {}
