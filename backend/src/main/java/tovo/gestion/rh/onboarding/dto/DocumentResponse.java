package tovo.gestion.rh.onboarding.dto;

import tovo.gestion.rh.onboarding.entity.DocumentEmploye;

import java.time.LocalDateTime;
import java.time.LocalDate;

public record DocumentResponse(
        Long id, Long employeId, Long onboardingTacheId, String type,
        String nomFichier, String url, Long taille, LocalDateTime dateDepot,
        String statut, LocalDate dateEmission, LocalDate dateExpiration, Integer version
) {
    public static DocumentResponse from(DocumentEmploye d) {
        return new DocumentResponse(d.getId(), d.getEmploye().getId(),
                d.getOnboardingTache() == null ? null : d.getOnboardingTache().getId(),
                d.getType(), d.getNomFichier(), d.getUrl(), d.getTaille(), d.getDateDepot(),
                d.getStatut(), d.getDateEmission(), d.getDateExpiration(), d.getVersion());
    }
}
