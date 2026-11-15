package tovo.gestion.rh.departement.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DepartementResponse {
    private Integer id;
    private String nom;
    private String description;
}