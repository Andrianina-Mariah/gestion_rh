package tovo.gestion.rh.onboarding.dto;

import jakarta.validation.constraints.NotNull;

public record ReassignerTacheRequest(
        @NotNull Integer assigneeId,
        String responsableRole
) {
}
