package tovo.gestion.rh.workflow.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import tovo.gestion.rh.workflow.DecisionValidation;

import java.math.BigDecimal;

@Data
public class DecisionValidationRequest {

    @NotNull(message = "La décision est obligatoire")
    private DecisionValidation decision;

    private String commentaire;

    private Boolean analyseBudget;

    private BigDecimal budgetValide;
}
