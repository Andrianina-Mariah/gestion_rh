package tovo.gestion.rh.conge.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity @Table(name = "type_conge") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TypeConge {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, unique=true, length=40) private String code;
    @Column(nullable=false, length=150) private String libelle;
    @Column(nullable=false) private Boolean remunere;
    @Column(name="debite_solde", nullable=false) private Boolean debiteSolde;
    @Column(name="justificatif_obligatoire", nullable=false) private Boolean justificatifObligatoire;
    @Column(name="justificatif_apres_jours", precision=6, scale=2) private BigDecimal justificatifApresJours;
    @Column(name="duree_max_jours", precision=7, scale=2) private BigDecimal dureeMaxJours;
    @Column(name="demi_journee_autorisee", nullable=false) private Boolean demiJourneeAutorisee;
    @Column(name="unite_decompte", nullable=false, length=12) private String uniteDecompte;
    @Column(nullable=false) private Boolean actif;
    @Column(name="solde_negatif_autorise", nullable=false) private Boolean soldeNegatifAutorise;
}
