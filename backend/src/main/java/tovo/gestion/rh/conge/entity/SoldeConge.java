package tovo.gestion.rh.conge.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity @Table(name="solde_conge") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SoldeConge {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="employe_id", nullable=false) private Long employeId;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="type_conge_id", nullable=false) private TypeConge typeConge;
    @Column(nullable=false) private Integer annee;
    @Column(nullable=false, precision=8, scale=2) private BigDecimal acquis;
    @Column(nullable=false, precision=8, scale=2) private BigDecimal pris;
    @Column(name="en_attente", nullable=false, precision=8, scale=2) private BigDecimal enAttente;
    @Column(nullable=false, precision=8, scale=2) private BigDecimal reporte;
    @Column(nullable=false, precision=8, scale=2) private BigDecimal ajuste;
}
