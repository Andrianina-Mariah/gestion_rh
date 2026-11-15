package tovo.gestion.rh.onboarding.entity;

import jakarta.persistence.*;
import lombok.*;
import tovo.gestion.rh.embauche.entity.Employe;
import tovo.gestion.rh.utilisateur.entity.Utilisateur;

import java.time.LocalDate;

@Entity
@Table(name = "onboarding")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Onboarding {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "employe_id", nullable = false, unique = true)
    private Employe employe;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatutOnboarding statut = StatutOnboarding.A_FAIRE;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin_prevue")
    private LocalDate dateFinPrevue;

    @Column(name = "date_cloture")
    private LocalDate dateCloture;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_id")
    private Utilisateur responsable;
}
