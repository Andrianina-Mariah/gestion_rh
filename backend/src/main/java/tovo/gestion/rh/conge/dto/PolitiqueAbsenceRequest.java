package tovo.gestion.rh.conge.dto;
import jakarta.validation.constraints.*;
public record PolitiqueAbsenceRequest(@NotNull @Min(0) Integer delaiJustificatifJours,
 @NotNull @Min(1) @Max(52428800) Long tailleMaxJustificatifOctets) {}
