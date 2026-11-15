package tovo.gestion.rh.onboarding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreerOnboardingTacheRequest(
        @NotBlank @Size(max = 200) String libelle,
        @NotBlank @Size(max = 50) String responsableRole,
        @NotNull LocalDate dateEcheance,
        boolean obligatoire
) {
}
