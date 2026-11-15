package tovo.gestion.rh.onboarding.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "onboarding_modele_tache")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OnboardingModeleTache {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "modele_id", nullable = false)
    private OnboardingModele modele;

    @Column(nullable = false, length = 200)
    private String libelle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TypeOnboardingTache type;

    @Column(name = "responsable_role", nullable = false, length = 50)
    private String responsableRole;

    @Column(name = "delai_jours", nullable = false)
    private Integer delaiJours;

    @Column(nullable = false)
    @Builder.Default
    private Boolean obligatoire = true;
}
