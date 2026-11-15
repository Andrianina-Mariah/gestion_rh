package tovo.gestion.rh.embauche.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record AdresseRequest(@NotBlank String type, @NotBlank String adresse,
                             String ville, @NotNull LocalDate dateDebut) {}
