package tovo.gestion.rh.onboarding.dto;

import jakarta.validation.constraints.NotBlank;

public record CommentaireTacheRequest(@NotBlank String commentaire) {
}
