package tovo.gestion.rh.workflow.dto;

import lombok.Builder;
import lombok.Data;
import tovo.gestion.rh.workflow.DecisionValidation;
import tovo.gestion.rh.workflow.EtapeValidation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ValidationHistoryItem {
    private Integer id;
    private EtapeValidation etape;
    private Integer validateurId;
    private String validateurNom;
    private DecisionValidation statut;
    private String commentaire;
    private BigDecimal budgetValide;
    private Integer cycle;
    private LocalDateTime dateValidation;
}
