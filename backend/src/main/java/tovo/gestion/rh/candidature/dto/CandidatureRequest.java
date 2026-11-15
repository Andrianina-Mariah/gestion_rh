package tovo.gestion.rh.candidature.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CandidatureRequest(
        @NotBlank String nom,
        @NotBlank String prenom,
        @NotBlank @Email String email,
        String telephone,
        String adresse,
        @NotBlank String titreProfil,
        String resume,
        String competences,
        String langues,
        @Valid List<ExperienceRequest> experiences,
        @Valid List<FormationRequest> formations
) {}
