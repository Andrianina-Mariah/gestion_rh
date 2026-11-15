package tovo.gestion.rh.embauche.dto;

import tovo.gestion.rh.embauche.entity.Contrat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ContratResponse(
        Long id,
        Long employeId,
        String typeContrat,
        String statut,
        LocalDate dateDebut,
        LocalDate dateFin,
        BigDecimal salaire,
        String motif,
        String contratUrl,
        LocalDateTime dateCreation,
        Integer numeroRenouvellement
) {
    public static ContratResponse from(Contrat contrat) {
        return new ContratResponse(
                contrat.getId(),
                contrat.getEmploye() != null ? contrat.getEmploye().getId() : null,
                contrat.getTypeContrat() == null ? null : contrat.getTypeContrat().name(),
                contrat.getStatut() == null ? null : contrat.getStatut().name(),
                contrat.getDateDebut(),
                contrat.getDateFin(),
                contrat.getSalaire(),
                contrat.getMotif(),
                contrat.getContratUrl(),
                contrat.getDateCreation(),
                contrat.getNumeroRenouvellement()
        );
    }
}
