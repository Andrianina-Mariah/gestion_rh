package tovo.gestion.rh.demanderecrutement.dto;

import lombok.Builder;
import lombok.Data;
import tovo.gestion.rh.demanderecrutement.entity.StatutDemande;
import tovo.gestion.rh.demanderecrutement.entity.TypeDemande;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class DemandeListItem {
    private Integer id;
    private String posteTitre;
    private String departementNom;
    private String demandeurNom;
    private Integer nombrePostes;
    private TypeDemande type;
    private BigDecimal budget;
    private LocalDate dateSouhaitee;
    private StatutDemande statut;
    private LocalDateTime dateCreation;
}