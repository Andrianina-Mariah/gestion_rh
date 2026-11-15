package tovo.gestion.rh.onboarding.dto;

import tovo.gestion.rh.onboarding.entity.MaterielAttribue;
import java.time.LocalDate;

public record MaterielResponse(Long id, Long employeId, Long tacheId, String typeMateriel,
                               String description, String numeroSerie, LocalDate dateAttribution) {
    public static MaterielResponse from(MaterielAttribue m) {
        return new MaterielResponse(m.getId(), m.getEmploye().getId(),
                m.getOnboardingTache() == null ? null : m.getOnboardingTache().getId(),
                m.getTypeMateriel(), m.getDescription(), m.getNumeroSerie(), m.getDateAttribution());
    }
}
