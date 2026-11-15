package tovo.gestion.rh.onboarding.dto;

import tovo.gestion.rh.onboarding.entity.OnboardingTache;

import java.time.LocalDate;

public record OnboardingTacheResponse(
        Long id, String libelle, String type, String statut, String responsableRole,
        Integer assigneeId, String assigneeNom, LocalDate dateEcheance,
        LocalDate dateRealisation, boolean obligatoire, String commentaire
) {
    public static OnboardingTacheResponse from(OnboardingTache t) {
        return new OnboardingTacheResponse(
                t.getId(), t.getLibelle(), t.getType().name(), t.getStatut().name(),
                t.getResponsableRole(),
                t.getAssignee() == null ? null : t.getAssignee().getId(),
                t.getAssignee() == null ? null : t.getAssignee().getPrenom() + " " + t.getAssignee().getNom(),
                t.getDateEcheance(), t.getDateRealisation(),
                Boolean.TRUE.equals(t.getObligatoire()), t.getCommentaire());
    }
}
