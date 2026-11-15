package tovo.gestion.rh.poste.mapper;

import org.springframework.stereotype.Component;
import tovo.gestion.rh.departement.entity.Departement;
import tovo.gestion.rh.poste.dto.PosteRequest;
import tovo.gestion.rh.poste.dto.PosteResponse;
import tovo.gestion.rh.poste.entity.Poste;

@Component
public class PosteMapper {

    public Poste toEntity(PosteRequest request, Departement departement) {
        return Poste.builder()
                .titre(request.getTitre())
                .departement(departement)
                .descriptionProfil(request.getDescriptionProfil())
                .build();
    }

    public PosteResponse toResponse(Poste entity) {
        return PosteResponse.builder()
                .id(entity.getId())
                .titre(entity.getTitre())
                .departementId(entity.getDepartement().getId())
                .departementNom(entity.getDepartement().getNom())
                .descriptionProfil(entity.getDescriptionProfil())
                .build();
    }

    public void updateEntity(Poste entity, PosteRequest request, Departement departement) {
        entity.setTitre(request.getTitre());
        entity.setDepartement(departement);
        entity.setDescriptionProfil(request.getDescriptionProfil());
    }
}