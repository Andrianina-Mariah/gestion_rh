package tovo.gestion.rh.embauche.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SalaireRequest(@NotNull @DecimalMin("0.01") BigDecimal montant,
                             @Pattern(regexp = "MGA|EUR|USD") String devise,
                             @NotNull LocalDate dateDebut, String motif, Long contratId) {}
