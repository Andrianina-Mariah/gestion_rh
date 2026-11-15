package tovo.gestion.rh.conge.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity @Table(name="mouvement_solde") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MouvementSolde {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="solde_conge_id", nullable=false) private SoldeConge soldeConge;
    @Column(nullable=false, length=20) private String type;
    @Column(nullable=false, precision=8, scale=2) private BigDecimal quantite;
    @Column(name="demande_id") private Long demandeId;
    @Column(nullable=false) private LocalDate date;
    private String motif;
    @Column(name="cree_par_id") private Integer creeParId;
    @Column(name="cle_idempotence", length=120) private String cleIdempotence;
}
