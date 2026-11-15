package tovo.gestion.rh.onboarding.entity;

import jakarta.persistence.*;
import lombok.*;
import tovo.gestion.rh.embauche.entity.Employe;

import java.time.LocalDate;

@Entity
@Table(name = "materiel_attribue")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MaterielAttribue {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employe_id", nullable = false)
    private Employe employe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "onboarding_tache_id")
    private OnboardingTache onboardingTache;

    @Column(name = "type_materiel", nullable = false, length = 80)
    private String typeMateriel;
    @Column(length = 200)
    private String description;
    @Column(name = "numero_serie", nullable = false, unique = true, length = 120)
    private String numeroSerie;
    @Column(name = "date_attribution", nullable = false)
    private LocalDate dateAttribution;
}
