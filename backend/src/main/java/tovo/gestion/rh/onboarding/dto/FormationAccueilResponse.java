package tovo.gestion.rh.onboarding.dto;

import tovo.gestion.rh.onboarding.entity.FormationAccueil;
import java.math.BigDecimal;
import java.time.LocalDate;

public record FormationAccueilResponse(Long id, Long employeId, Long onboardingId, Long tacheId,
                                       LocalDate dateFormation, BigDecimal dureeHeures, String formateur) {
    public static FormationAccueilResponse from(FormationAccueil f) {
        return new FormationAccueilResponse(f.getId(), f.getEmploye().getId(), f.getOnboarding().getId(),
                f.getOnboardingTache() == null ? null : f.getOnboardingTache().getId(),
                f.getDateFormation(), f.getDureeHeures(), f.getFormateur());
    }
}
