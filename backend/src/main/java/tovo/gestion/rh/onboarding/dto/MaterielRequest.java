package tovo.gestion.rh.onboarding.dto;

import jakarta.validation.constraints.NotBlank;

public record MaterielRequest(
        @NotBlank String typeMateriel, String description, @NotBlank String numeroSerie,
        String commentaire
) {}
