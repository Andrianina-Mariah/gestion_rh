package tovo.gestion.rh.embauche.dto;

import tovo.gestion.rh.embauche.entity.DemandeModification;

public record DemandeModificationResponse(Long id, Long employeId, String champ,
        String ancienneValeur, String nouvelleValeur, String statut, String commentaire) {
    public static DemandeModificationResponse from(DemandeModification d) {
        return new DemandeModificationResponse(d.getId(), d.getEmploye().getId(), d.getChamp(),
                d.getAncienneValeur(), d.getNouvelleValeur(), d.getStatut().name(), d.getCommentaire());
    }
}
