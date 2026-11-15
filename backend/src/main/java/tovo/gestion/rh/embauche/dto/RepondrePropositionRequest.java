package tovo.gestion.rh.embauche.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RepondrePropositionRequest(
        @NotBlank String jeton,
        @NotNull ReponseCandidat reponse,
        String remarque,
        String souhaits
) {
    public enum ReponseCandidat { ACCEPTER, REFUSER, NEGOCIER }
}
