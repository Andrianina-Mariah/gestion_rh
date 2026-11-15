package tovo.gestion.rh.candidature.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record ExperienceRequest(
        @NotBlank String poste,
        @NotBlank String entreprise,
        LocalDate dateDebut,
        LocalDate dateFin,
        boolean enPoste,
        String description
) {}
