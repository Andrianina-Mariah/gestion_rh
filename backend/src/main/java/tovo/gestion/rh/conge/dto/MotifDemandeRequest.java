package tovo.gestion.rh.conge.dto;
import jakarta.validation.constraints.*;
public record MotifDemandeRequest(@NotBlank @Size(max=1000) String motif) {}
