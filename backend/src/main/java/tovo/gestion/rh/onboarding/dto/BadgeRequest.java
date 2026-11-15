package tovo.gestion.rh.onboarding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record BadgeRequest(@NotBlank String numero, @NotNull LocalDate dateRemise, String commentaire) {}
