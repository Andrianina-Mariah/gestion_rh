package tovo.gestion.rh.candidature.dto;

import jakarta.validation.constraints.NotBlank;

public record FormationRequest(
        @NotBlank String diplome,
        @NotBlank String etablissement,
        Integer anneeObtention,
        String description
) {}
