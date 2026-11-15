package tovo.gestion.rh.demanderecrutement.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import tovo.gestion.rh.demanderecrutement.entity.TypeDemande;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DemandeUpdateRequest {

    @NotNull(message = "Le poste est obligatoire")
    private Integer posteId;

    @NotNull(message = "Le type est obligatoire")
    private TypeDemande type;

    @NotNull(message = "Le nombre de postes est obligatoire")
    @Min(value = 1, message = "Le nombre de postes doit être au moins 1")
    private Integer nombrePostes;

    @NotBlank(message = "Le motif est obligatoire")
    private String motif;

    @NotNull(message = "Le budget est obligatoire")
    @DecimalMin(value = "0.01", message = "Le budget doit être supérieur à 0")
    private BigDecimal budget;

    @NotNull(message = "La date souhaitée est obligatoire")
    @Future(message = "La date souhaitée doit être postérieure à aujourd'hui")
    private LocalDate dateSouhaitee;
}