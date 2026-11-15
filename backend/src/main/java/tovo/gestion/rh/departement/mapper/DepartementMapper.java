package tovo.gestion.rh.departement.mapper;

import org.springframework.stereotype.Component;
import tovo.gestion.rh.departement.dto.DepartementRequest;
import tovo.gestion.rh.departement.dto.DepartementResponse;
import tovo.gestion.rh.departement.entity.Departement;

@Component
public class DepartementMapper {

    public Departement toEntity(DepartementRequest request) {
        return Departement.builder()
                .nom(request.getNom())
                .description(request.getDescription())
                .build();
    }

    public DepartementResponse toResponse(Departement entity) {
        return DepartementResponse.builder()
                .id(entity.getId())
                .nom(entity.getNom())
                .description(entity.getDescription())
                .build();
    }

    public void updateEntity(Departement entity, DepartementRequest request) {
        entity.setNom(request.getNom());
        entity.setDescription(request.getDescription());
    }
}