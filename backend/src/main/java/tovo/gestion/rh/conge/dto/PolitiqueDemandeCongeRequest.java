package tovo.gestion.rh.conge.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record PolitiqueDemandeCongeRequest(@NotNull @Min(0) Integer preavisMinJours,
 @DecimalMin("0") @DecimalMax("100") BigDecimal presenceMinPct,
 @Min(1) Integer rappelValidationJours, @DecimalMin("0") BigDecimal seuilSoldeFaibleJours) {}
