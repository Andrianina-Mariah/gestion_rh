package tovo.gestion.rh.embauche.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record AyantDroitRequest(@NotBlank String nom, @NotBlank String lien, LocalDate dateNaissance) {}
