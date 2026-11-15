package tovo.gestion.rh.candidature.dto;

import tovo.gestion.rh.candidature.entity.*;

import java.time.LocalDateTime;
import java.util.List;

public record CandidatureResponse(
        Long id,
        Long offreId,
        Long candidatId,
        String nom,
        String prenom,
        String email,
        String telephone,
        String titreProfil,
        String resume,
        String competences,
        String langues,
        String cvUrl,
        String lettreMotivationUrl,
        String statut,
        Integer rangAttente,
        LocalDateTime dateCandidature,
        List<HistoriqueResponse> historique
) {
    public static CandidatureResponse from(Candidature c, List<HistoriqueResponse> historique) {
        CvCandidature cv = c.getCv();
        return new CandidatureResponse(c.getId(), c.getOffre().getId(), c.getCandidat().getId(),
                c.getCandidat().getNom(), c.getCandidat().getPrenom(), c.getCandidat().getEmail(),
                c.getCandidat().getTelephone(), cv == null ? null : cv.getTitreProfil(),
                cv == null ? null : cv.getResume(), cv == null ? null : cv.getCompetences(),
                cv == null ? null : cv.getLangues(), c.getCvUrl(), c.getLettreMotivationUrl(),
                c.getStatut().name(), c.getRangAttente(), c.getDateCandidature(), historique);
    }
}
