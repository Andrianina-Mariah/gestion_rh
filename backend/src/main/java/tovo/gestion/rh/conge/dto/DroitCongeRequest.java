package tovo.gestion.rh.conge.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record DroitCongeRequest(@NotNull Long typeCongeId, @Pattern(regexp="CE|CDD|CDI") String typeContrat,
 @NotNull @DecimalMin("0") BigDecimal regleAcquisition, @NotBlank String periodeAcquisition,
 @NotNull @Min(0) Integer ancienneteMinMois, @NotNull @DecimalMin("0") BigDecimal plafondReport,
 @Min(1) Integer validiteReportMois) {}
