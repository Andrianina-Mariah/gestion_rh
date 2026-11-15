package tovo.gestion.rh.embauche.dto;

import jakarta.validation.constraints.NotBlank;

public record ModificationRequest(@NotBlank String champ, String ancienneValeur,
                                   @NotBlank String nouvelleValeur, String commentaire) {}
