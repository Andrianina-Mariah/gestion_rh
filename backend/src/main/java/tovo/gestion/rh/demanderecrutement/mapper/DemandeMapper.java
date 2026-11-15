package tovo.gestion.rh.demanderecrutement.mapper;

import org.springframework.stereotype.Component;
import tovo.gestion.rh.demanderecrutement.dto.*;
import tovo.gestion.rh.demanderecrutement.entity.DemandeRecrutement;
import tovo.gestion.rh.demanderecrutement.entity.StatutDemande;
import tovo.gestion.rh.poste.entity.Poste;
import tovo.gestion.rh.utilisateur.entity.Utilisateur;

@Component
public class DemandeMapper {

    public DemandeRecrutement toEntity(DemandeCreateRequest request, Poste poste, Utilisateur demandeur) {
        return DemandeRecrutement.builder()
                .poste(poste)
                .demandeur(demandeur)
                .nombrePostes(request.getNombrePostes())
                .type(request.getType())
                .motif(request.getMotif())
                .budget(request.getBudget())
                .dateSouhaitee(request.getDateSouhaitee())
                .statut(StatutDemande.BROUILLON)
                .build();
    }

    public void updateEntity(DemandeRecrutement entity, DemandeUpdateRequest request, Poste poste) {
        entity.setPoste(poste);
        entity.setNombrePostes(request.getNombrePostes());
        entity.setType(request.getType());
        entity.setMotif(request.getMotif());
        entity.setBudget(request.getBudget());
        entity.setDateSouhaitee(request.getDateSouhaitee());
    }

    public DemandeResponse toResponse(DemandeRecrutement entity) {
        return DemandeResponse.builder()
                .id(entity.getId())
                .posteId(entity.getPoste().getId())
                .posteTitre(entity.getPoste().getTitre())
                .departementNom(entity.getPoste().getDepartement().getNom())
                .demandeurId(entity.getDemandeur().getId())
                .demandeurNom(entity.getDemandeur().getPrenom() + " " + entity.getDemandeur().getNom())
                .nombrePostes(entity.getNombrePostes())
                .type(entity.getType())
                .motif(entity.getMotif())
                .budget(entity.getBudget())
                .dateSouhaitee(entity.getDateSouhaitee())
                .statut(entity.getStatut())
                .dateCreation(entity.getDateCreation())
                .dateModification(entity.getDateModification())
                .build();
    }

    public DemandeListItem toListItem(DemandeRecrutement entity) {
        return DemandeListItem.builder()
                .id(entity.getId())
                .posteTitre(entity.getPoste().getTitre())
                .departementNom(entity.getPoste().getDepartement().getNom())
                .demandeurNom(entity.getDemandeur().getPrenom() + " " + entity.getDemandeur().getNom())
                .nombrePostes(entity.getNombrePostes())
                .type(entity.getType())
                .budget(entity.getBudget())
                .dateSouhaitee(entity.getDateSouhaitee())
                .statut(entity.getStatut())
                .dateCreation(entity.getDateCreation())
                .build();
    }
}