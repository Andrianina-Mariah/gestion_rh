package tovo.gestion.rh.embauche.entity;

import jakarta.persistence.*;
import lombok.*;
import tovo.gestion.rh.departement.entity.Departement;
import tovo.gestion.rh.poste.entity.Poste;

import java.time.LocalDate;

@Entity
@Table(name = "affectation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Affectation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employe_id", nullable = false)
    private Employe employe;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "poste_id", nullable = false)
    private Poste poste;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "departement_id", nullable = false)
    private Departement departement;

    @Column(name = "date_affectation", nullable = false)
    private LocalDate dateAffectation;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Employe manager;
}
