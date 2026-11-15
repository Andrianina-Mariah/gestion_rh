package tovo.gestion.rh.embauche.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record SortieRequest(@NotBlank String motif, @NotNull LocalDate dateSortie) {}
