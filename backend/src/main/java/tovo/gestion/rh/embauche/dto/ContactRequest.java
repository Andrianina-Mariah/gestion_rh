package tovo.gestion.rh.embauche.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record ContactRequest(@NotBlank String type, @NotBlank String valeur,
                             boolean principal, @NotNull LocalDate dateDebut) {}
