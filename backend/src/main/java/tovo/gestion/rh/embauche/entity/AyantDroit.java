package tovo.gestion.rh.embauche.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "ayant_droit")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AyantDroit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "employe_id") private Employe employe;
    @Column(nullable = false, length = 150) private String nom;
    @Column(nullable = false, length = 50) private String lien;
    @Column(name = "date_naissance") private LocalDate dateNaissance;
}
