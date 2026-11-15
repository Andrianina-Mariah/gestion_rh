package tovo.gestion.rh.embauche.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "employe_adresse")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmployeAdresse {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "employe_id") private Employe employe;
    @Column(nullable = false, length = 20) private String type;
    @Column(nullable = false, length = 500) private String adresse;
    @Column(length = 120) private String ville;
    @Column(name = "date_debut", nullable = false) private LocalDate dateDebut;
    @Column(name = "date_fin") private LocalDate dateFin;
}
