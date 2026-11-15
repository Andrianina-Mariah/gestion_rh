package tovo.gestion.rh.conge.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity @Table(name="droit_conge") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DroitConge {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="type_conge_id", nullable=false) private TypeConge typeConge;
    @Column(name="type_contrat", length=3) private String typeContrat;
    @Column(name="regle_acquisition", nullable=false, precision=7, scale=3) private BigDecimal regleAcquisition;
    @Column(name="periode_acquisition", nullable=false, length=10) private String periodeAcquisition;
    @Column(name="anciennete_min_mois", nullable=false) private Integer ancienneteMinMois;
    @Column(name="plafond_report", nullable=false, precision=7, scale=2) private BigDecimal plafondReport;
    @Column(name="validite_report_mois") private Integer validiteReportMois;
}
