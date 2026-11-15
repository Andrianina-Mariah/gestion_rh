package tovo.gestion.rh.embauche.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "employe_contact")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmployeContact {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "employe_id") private Employe employe;
    @Column(nullable = false, length = 20) private String type;
    @Column(nullable = false, length = 180) private String valeur;
    @Column(nullable = false) @Builder.Default private Boolean principal = false;
    @Column(name = "date_debut", nullable = false) private LocalDate dateDebut;
    @Column(name = "date_fin") private LocalDate dateFin;
}
