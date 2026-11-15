package tovo.gestion.rh.embauche.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record AffectationRequest(@NotNull Integer posteId, @NotNull Integer departementId,
                                 Long managerId, @NotNull LocalDate dateAffectation) {}
