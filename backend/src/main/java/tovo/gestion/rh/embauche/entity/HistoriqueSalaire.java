package tovo.gestion.rh.embauche.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "historique_salaire")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HistoriqueSalaire {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "employe_id") private Employe employe;
    @Column(nullable = false, precision = 15, scale = 2) private BigDecimal montant;
    @Column(nullable = false, length = 3) @Builder.Default private String devise = "MGA";
    @Column(name = "date_debut", nullable = false) private LocalDate dateDebut;
    @Column(name = "date_fin") private LocalDate dateFin;
    @Column(length = 150) private String motif;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "contrat_id") private Contrat contrat;
}
