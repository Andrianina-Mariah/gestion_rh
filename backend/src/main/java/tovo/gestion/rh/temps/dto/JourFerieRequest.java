package tovo.gestion.rh.temps.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record JourFerieRequest(@NotNull LocalDate date, @NotBlank @Size(max = 150) String libelle,
        @NotNull Boolean paye) {}
