package tovo.gestion.rh.offreemploi.dto;

import tovo.gestion.rh.offreemploi.entity.OffreEmploi;

import java.time.LocalDateTime;
import java.math.BigDecimal;

public record OffreEmploiResponse(
        Long id,
        Integer demandeRecrutementId,
        String titreOffre,
        String descriptionPoste,
        String profilRecherche,
        String lieuTravail,
        String typeContratPropose,
        String statut,
        LocalDateTime datePublication,
        LocalDateTime dateCloture,
        Integer nombrePostes,
        BigDecimal budget
) {
    public static OffreEmploiResponse from(OffreEmploi offre) {
        return new OffreEmploiResponse(
                offre.getId(),
                offre.getDemandeRecrutement().getId(),
                offre.getTitreOffre(),
                offre.getDescriptionPoste(),
                offre.getProfilRecherche(),
                offre.getLieuTravail(),
                offre.getTypeContratPropose(),
                offre.getStatut().name(),
                offre.getDatePublication(),
                offre.getDateCloture(),
                offre.getDemandeRecrutement().getNombrePostes(),
                offre.getDemandeRecrutement().getBudget()
        );
    }
}
