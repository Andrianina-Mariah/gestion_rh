package tovo.gestion.rh.onboarding.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FormationAccueilRequest(
        @NotNull LocalDate dateFormation,
        @NotNull @DecimalMin("0.01") BigDecimal dureeHeures,
        @NotBlank String formateur,
        String commentaire
) {}
