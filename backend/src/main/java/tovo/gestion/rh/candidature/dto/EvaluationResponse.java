package tovo.gestion.rh.candidature.dto;

import tovo.gestion.rh.candidature.entity.Evaluation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EvaluationResponse(
        Long id, Long candidatureId, String type, LocalDateTime date,
        LocalDateTime dateRealisation, Integer evaluateurId, BigDecimal note,
        String remarque, String resultat
) {
    public static EvaluationResponse from(Evaluation e) {
        return new EvaluationResponse(e.getId(), e.getCandidature().getId(),
                e.getType().name(), e.getDatePlanifiee(), e.getDateRealisation(),
                e.getEvaluateur() == null ? null : e.getEvaluateur().getId(),
                e.getNote(), e.getRemarque(),
                e.getResultat() == null ? null : e.getResultat().name());
    }
}
