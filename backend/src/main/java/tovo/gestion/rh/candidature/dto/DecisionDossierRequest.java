package tovo.gestion.rh.candidature.dto;

import jakarta.validation.constraints.NotNull;
import tovo.gestion.rh.candidature.entity.DecisionDossier;

public record DecisionDossierRequest(
        @NotNull(message = "La décision est obligatoire")
        DecisionDossier decision,
        String commentaire
) {
}
