package tovo.gestion.rh.conge.dto;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
public record PeriodeBlocageCongeRequest(@NotNull LocalDate dateDebut,@NotNull LocalDate dateFin,@NotBlank @Size(max=200) String libelle) {}
