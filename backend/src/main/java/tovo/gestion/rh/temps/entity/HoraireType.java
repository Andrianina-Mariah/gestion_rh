package tovo.gestion.rh.temps.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;

@Entity @Table(name = "horaire_type") @Getter @Setter @NoArgsConstructor
public class HoraireType {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "regime_travail_id") private RegimeTravail regimeTravail;
    @Column(name = "jour_semaine", nullable = false) private Integer jourSemaine;
    @Column(name = "heure_debut", nullable = false) private LocalTime heureDebut;
    @Column(name = "heure_fin", nullable = false) private LocalTime heureFin;
    @Column(name = "pause_minutes", nullable = false) private Integer pauseMinutes;
    @Column(nullable = false) private Boolean travaille;
}
