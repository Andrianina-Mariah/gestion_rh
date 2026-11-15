package tovo.gestion.rh.candidature.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DecisionFinaleRequest(
        @NotNull DecisionFinale decision,
        @NotBlank(message = "Le commentaire est obligatoire") String commentaire
) {
    public enum DecisionFinale { RETENU, REJETE }
}
