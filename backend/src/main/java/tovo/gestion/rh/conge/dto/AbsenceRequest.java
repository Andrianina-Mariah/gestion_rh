package tovo.gestion.rh.conge.dto;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
public record AbsenceRequest(@NotNull Long employeId,@NotBlank String type,@NotNull LocalDate dateDebut,
 @NotNull LocalDate dateFin,@Size(max=2000) String commentaire) {}
