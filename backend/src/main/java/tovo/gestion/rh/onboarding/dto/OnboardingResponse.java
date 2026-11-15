package tovo.gestion.rh.onboarding.dto;

import tovo.gestion.rh.onboarding.entity.Onboarding;

import java.time.LocalDate;
import java.util.List;

public record OnboardingResponse(
        Long id, Long employeId, String matricule, String employeNom,
        String statut, LocalDate dateDebut, LocalDate dateFinPrevue,
        LocalDate dateCloture, int progression, List<OnboardingTacheResponse> taches
) {
    public static OnboardingResponse from(Onboarding onboarding, List<OnboardingTacheResponse> taches) {
        int total = taches.size();
        long faites = taches.stream().filter(t -> "FAIT".equals(t.statut())
                || "NON_APPLICABLE".equals(t.statut())).count();
        return new OnboardingResponse(
                onboarding.getId(), onboarding.getEmploye().getId(),
                onboarding.getEmploye().getMatricule(),
                onboarding.getEmploye().getCandidat().getPrenom() + " "
                        + onboarding.getEmploye().getCandidat().getNom(),
                onboarding.getStatut().name(), onboarding.getDateDebut(),
                onboarding.getDateFinPrevue(), onboarding.getDateCloture(),
                total == 0 ? 0 : (int) Math.round(faites * 100.0 / total), taches);
    }
}
