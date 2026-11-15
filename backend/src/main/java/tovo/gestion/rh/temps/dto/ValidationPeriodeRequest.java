package tovo.gestion.rh.temps.dto;

import jakarta.validation.constraints.Size;

public record ValidationPeriodeRequest(@Size(max=2000) String motifDerogation) {}
