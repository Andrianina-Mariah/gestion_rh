package tovo.gestion.rh.temps.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity @Table(name = "regime_travail") @Getter @Setter @NoArgsConstructor
public class RegimeTravail {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 40) private String code;
    @Column(nullable = false, length = 150) private String libelle;
    @Column(name = "heures_hebdo", nullable = false, precision = 5, scale = 2) private BigDecimal heuresHebdo;
    @Column(name = "jours_travailles_par_semaine", nullable = false, precision = 3, scale = 1) private BigDecimal joursTravaillesParSemaine;
    @Column(name = "tolerance_retard_min", nullable = false) private Integer toleranceRetardMin;
}
