package tovo.gestion.rh.temps.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record RegimeRequest(@NotBlank @Size(max = 40) String code,
        @NotBlank @Size(max = 150) String libelle,
        @NotNull @DecimalMin("0.01") @DecimalMax("168") BigDecimal heuresHebdo,
        @NotNull @DecimalMin("0.1") @DecimalMax("7") BigDecimal joursTravaillesParSemaine,
        @NotNull @Min(0) Integer toleranceRetardMin) {}
