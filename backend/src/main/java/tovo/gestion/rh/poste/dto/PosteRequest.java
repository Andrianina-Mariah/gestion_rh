package tovo.gestion.rh.poste.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PosteRequest {

    @NotBlank(message = "Le titre du poste est obligatoire")
    @Size(max = 150, message = "Le titre ne peut pas dépasser 150 caractères")
    private String titre;

    @NotNull(message = "Le département est obligatoire")
    private Integer departementId;

    private String descriptionProfil;
}