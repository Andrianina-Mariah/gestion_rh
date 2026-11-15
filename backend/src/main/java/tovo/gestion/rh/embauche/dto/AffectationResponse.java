package tovo.gestion.rh.embauche.dto;

import tovo.gestion.rh.embauche.entity.Affectation;

import java.time.LocalDate;

public record AffectationResponse(
        Long id,
        Integer posteId,
        String poste,
        Integer departementId,
        String departement,
        Long managerId,
        LocalDate dateAffectation,
        LocalDate dateFin
) {
    public static AffectationResponse from(Affectation affectation) {
        return new AffectationResponse(
                affectation.getId(),
                affectation.getPoste() != null ? affectation.getPoste().getId() : null,
                affectation.getPoste() != null ? affectation.getPoste().getTitre() : null,
                affectation.getDepartement() != null ? affectation.getDepartement().getId() : null,
                affectation.getDepartement() != null ? affectation.getDepartement().getNom() : null,
                affectation.getManager() == null ? null : affectation.getManager().getId(),
                affectation.getDateAffectation(),
                affectation.getDateFin()
        );
    }
}
