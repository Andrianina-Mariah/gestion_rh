package tovo.gestion.rh.temps.entity;

import jakarta.persistence.*;
import lombok.*;
import tovo.gestion.rh.embauche.entity.Employe;
import java.time.LocalDate;

@Entity @Table(name = "employe_regime") @Getter @Setter @NoArgsConstructor
public class EmployeRegime {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "employe_id") private Employe employe;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "regime_travail_id") private RegimeTravail regimeTravail;
    @Column(name = "date_debut", nullable = false) private LocalDate dateDebut;
    @Column(name = "date_fin") private LocalDate dateFin;
}
