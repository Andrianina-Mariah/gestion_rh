package tovo.gestion.rh.temps.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreerPeriodeRequest(@NotNull @Min(1900) @Max(9999) Integer annee,
        @NotNull @Min(1) @Max(12) Integer mois) {}
