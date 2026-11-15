package tovo.gestion.rh.temps.dto;

import jakarta.validation.constraints.NotNull;

public record PointageRequest(@NotNull SensPointage sens) {
    public enum SensPointage { ENTREE, SORTIE }
}
