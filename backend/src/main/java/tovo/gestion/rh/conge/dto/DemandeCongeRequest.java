package tovo.gestion.rh.conge.dto;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
public record DemandeCongeRequest(@NotNull Long typeCongeId, @NotNull LocalDate dateDebut,
 @NotNull LocalDate dateFin, @NotNull Boolean demiJourneeDebut, @NotNull Boolean demiJourneeFin,
 @NotBlank @Size(max=3000) String motif) {}
