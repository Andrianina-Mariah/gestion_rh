package tovo.gestion.rh.temps.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record DemandeHeureSupRequest(Long employeId,@NotNull LocalDate date,
        @NotNull @Min(1) Integer minutes,@NotBlank @Size(max=1000) String motif) {}
