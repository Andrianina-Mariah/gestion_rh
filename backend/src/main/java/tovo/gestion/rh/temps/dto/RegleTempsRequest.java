package tovo.gestion.rh.temps.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalTime;

public record RegleTempsRequest(@NotNull LocalTime nuitDebut,@NotNull LocalTime nuitFin,
        @NotNull @DecimalMin("0") BigDecimal tauxSup30,@NotNull @DecimalMin("0") BigDecimal tauxSup50,
        @NotNull @Min(0) Integer seuilSup50Minutes,@NotNull @Min(1) Integer quotaSupHebdoMinutes,
        @NotNull @Min(1) Integer quotaSupAnnuelMinutes,@NotNull @Min(1) Integer dureeMaxJournaliereMinutes) {}
