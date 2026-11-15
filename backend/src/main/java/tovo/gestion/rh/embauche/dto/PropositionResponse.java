package tovo.gestion.rh.embauche.dto;

import tovo.gestion.rh.embauche.entity.PropositionEmbauche;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PropositionResponse(
        Long id, Long candidatureId, Integer version, Long propositionPrecedenteId,
        LocalDate dateDebut, String lieu, BigDecimal salaire, String typeContrat,
        String statut, String remarqueCandidat, String souhaitsCandidat,
        LocalDateTime dateProposition, LocalDateTime dateReponse,
        LocalDateTime jetonExpireLe, String jeton
) {
    public static PropositionResponse from(PropositionEmbauche p, String jeton) {
        return new PropositionResponse(
                p.getId(), p.getCandidature().getId(), p.getVersion(),
                p.getPropositionPrecedente() == null ? null : p.getPropositionPrecedente().getId(),
                p.getDateDebut(), p.getLieu(), p.getSalaire(), p.getTypeContrat().name(),
                p.getStatut().name(), p.getRemarqueCandidat(), p.getSouhaitsCandidat(),
                p.getDateProposition(), p.getDateReponse(), p.getJetonExpireLe(), jeton
        );
    }
}
