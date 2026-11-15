package tovo.gestion.rh.poste.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PosteResponse {
    private Integer id;
    private String titre;
    private Integer departementId;
    private String departementNom;
    private String descriptionProfil;
}