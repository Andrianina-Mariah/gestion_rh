package tovo.gestion.rh.conge.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record AjustementSoldeRequest(@NotNull Long typeCongeId, @NotNull @Min(2000) Integer annee,
 @NotNull @DecimalMin(value="-99999.99", inclusive=false) @DecimalMax(value="99999.99", inclusive=false) BigDecimal quantite,
 @NotBlank @Size(max=1000) String motif) {}
