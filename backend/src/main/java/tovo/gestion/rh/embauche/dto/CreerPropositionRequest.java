package tovo.gestion.rh.embauche.dto;

import jakarta.validation.constraints.*;
import tovo.gestion.rh.embauche.entity.TypeContratPropose;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreerPropositionRequest(
        @NotNull LocalDate dateDebut,
        @NotBlank @Size(max = 150) String lieu,
        @NotNull @DecimalMin(value = "0.01") BigDecimal salaire,
        @NotNull TypeContratPropose typeContrat
) {}
