package tovo.gestion.rh.onboarding.entity;

import jakarta.persistence.*;
import lombok.*;
import tovo.gestion.rh.embauche.entity.Employe;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "formation_accueil")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FormationAccueil {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employe_id", nullable = false)
    private Employe employe;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "onboarding_id", nullable = false)
    private Onboarding onboarding;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "onboarding_tache_id")
    private OnboardingTache onboardingTache;
    @Column(name = "date_formation", nullable = false)
    private LocalDate dateFormation;
    @Column(name = "duree_heures", nullable = false, precision = 6, scale = 2)
    private BigDecimal dureeHeures;
    @Column(nullable = false, length = 150)
    private String formateur;
}
