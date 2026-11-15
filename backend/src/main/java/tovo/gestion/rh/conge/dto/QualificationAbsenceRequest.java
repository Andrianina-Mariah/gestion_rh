package tovo.gestion.rh.conge.dto;
import jakarta.validation.constraints.*;
public record QualificationAbsenceRequest(@NotBlank String type,@Positive Long typeCongeId) {}
