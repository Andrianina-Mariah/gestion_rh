package tovo.gestion.rh.embauche.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ContactUrgenceRequest(@NotBlank String nom, @NotBlank String lienParente,
                                    @NotBlank @Pattern(regexp = "^[+0-9][0-9 .-]{7,20}$") String telephone,
                                    boolean principal) {}
