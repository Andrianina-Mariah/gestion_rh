package tovo.gestion.rh.temps.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReouverturePeriodeRequest(@NotBlank @Size(max=2000) String motif) {}
