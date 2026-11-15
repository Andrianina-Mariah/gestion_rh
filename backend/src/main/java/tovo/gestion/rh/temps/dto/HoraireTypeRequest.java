package tovo.gestion.rh.temps.dto;

import jakarta.validation.constraints.*;
import java.time.LocalTime;

public record HoraireTypeRequest(@NotNull @Min(1) @Max(7) Integer jourSemaine,
        @NotNull LocalTime heureDebut, @NotNull LocalTime heureFin,
        @NotNull @Min(0) Integer pauseMinutes, @NotNull Boolean travaille) {}
