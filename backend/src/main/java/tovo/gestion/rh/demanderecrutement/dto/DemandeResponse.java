package tovo.gestion.rh.demanderecrutement.dto;

import lombok.Builder;
import lombok.Data;
import tovo.gestion.rh.demanderecrutement.entity.StatutDemande;
import tovo.gestion.rh.demanderecrutement.entity.TypeDemande;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import tovo.gestion.rh.workflow.dto.ValidationHistoryItem;

@Data
@Builder
public class DemandeResponse {
    private Integer id;
    private Integer posteId;
    private String posteTitre;
    private String departementNom;
    private Integer demandeurId;
    private String demandeurNom;
    private Integer nombrePostes;
    private TypeDemande type;
    private String motif;
    private BigDecimal budget;
    private LocalDate dateSouhaitee;
    private StatutDemande statut;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    private List<ValidationHistoryItem> historique;
}