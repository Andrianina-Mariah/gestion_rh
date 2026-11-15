package tovo.gestion.rh.temps.dto;

import jakarta.validation.constraints.*;
import tovo.gestion.rh.temps.dto.PointageRequest.SensPointage;
import java.time.OffsetDateTime;

public record CorrectionPointageRequest(@NotNull Long employeId,
        Long pointageOrigineId, @NotNull OffsetDateTime dateHeure,
        @NotNull SensPointage sens, @NotBlank @Size(max=1000) String motif) {}
