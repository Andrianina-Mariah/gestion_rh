package tovo.gestion.rh.offreemploi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OffreEmploiRequest(
        @NotBlank(message = "Le titre de l'offre est obligatoire")
        @Size(max = 200, message = "Le titre ne peut pas dépasser 200 caractères")
        String titreOffre,

        @NotBlank(message = "La description du poste est obligatoire")
        String descriptionPoste,

        @NotBlank(message = "Le profil recherché est obligatoire")
        String profilRecherche,

        @Size(max = 100, message = "Le lieu ne peut pas dépasser 100 caractères")
        String lieuTravail,

        @Size(max = 50, message = "Le type de contrat ne peut pas dépasser 50 caractères")
        String typeContratPropose
) {
}
