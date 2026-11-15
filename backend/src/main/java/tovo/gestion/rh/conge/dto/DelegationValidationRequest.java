package tovo.gestion.rh.conge.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record DelegationValidationRequest(@NotNull Long delegueId,
                                          @NotNull LocalDate dateDebut,
                                          @NotNull LocalDate dateFin) {}
