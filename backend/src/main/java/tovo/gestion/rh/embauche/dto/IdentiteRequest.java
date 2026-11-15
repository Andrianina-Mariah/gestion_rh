package tovo.gestion.rh.embauche.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record IdentiteRequest(
        @NotNull @Past LocalDate dateNaissance,
        @Size(max = 150) String lieuNaissance,
        @Pattern(regexp = "FEMME|HOMME|AUTRE") String sexe,
        @Size(max = 80) String nationalite,
        @Pattern(regexp = "^[A-Z0-9 -]{5,30}$") String cin,
        @Pattern(regexp = "^[A-Z0-9 -]{5,30}$") String numeroCnaps,
        @Pattern(regexp = "CELIBATAIRE|MARIE|DIVORCE|VEUF|AUTRE") String situationFamiliale,
        @NotNull @Min(0) Integer nombreEnfants
) {}
