package tovo.gestion.rh.candidature.dto;

import tovo.gestion.rh.candidature.entity.CandidatureHistorique;

import java.time.LocalDateTime;

public record HistoriqueResponse(
        String ancienStatut,
        String nouveauStatut,
        String utilisateur,
        String commentaire,
        LocalDateTime date
) {
    public static HistoriqueResponse from(CandidatureHistorique h) {
        return new HistoriqueResponse(
                h.getAncienStatut() == null ? null : h.getAncienStatut().name(),
                h.getNouveauStatut().name(),
                h.getUtilisateur() == null ? null : h.getUtilisateur().getEmail(),
                h.getCommentaire(), h.getDate());
    }
}
