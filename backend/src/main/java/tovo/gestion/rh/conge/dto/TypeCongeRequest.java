package tovo.gestion.rh.conge.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record TypeCongeRequest(@NotBlank @Size(max=40) String code, @NotBlank @Size(max=150) String libelle,
 @NotNull Boolean remunere, @NotNull Boolean debiteSolde, @NotNull Boolean justificatifObligatoire,
 @DecimalMin("0") BigDecimal justificatifApresJours, @DecimalMin(value="0.01") BigDecimal dureeMaxJours,
 @NotNull Boolean demiJourneeAutorisee, @NotBlank String uniteDecompte, @NotNull Boolean actif,
 @NotNull Boolean soldeNegatifAutorise) {}
