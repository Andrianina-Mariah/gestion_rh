package tovo.gestion.rh.onboarding.dto;

import jakarta.validation.constraints.NotBlank;

public record InscriptionPaieRequest(
        @NotBlank String referencePaie,
        @NotBlank String modePaiement,
        @NotBlank String numeroCompte,
        String commentaire
) {}
