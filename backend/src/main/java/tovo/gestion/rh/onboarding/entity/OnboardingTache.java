package tovo.gestion.rh.onboarding.entity;

import jakarta.persistence.*;
import lombok.*;
import tovo.gestion.rh.utilisateur.entity.Utilisateur;

import java.time.LocalDate;

@Entity
@Table(name = "onboarding_tache")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OnboardingTache {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "onboarding_id", nullable = false)
    private Onboarding onboarding;

    @Column(nullable = false, length = 200)
    private String libelle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TypeOnboardingTache type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatutOnboardingTache statut = StatutOnboardingTache.A_FAIRE;

    @Column(name = "responsable_role", nullable = false, length = 50)
    private String responsableRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private Utilisateur assignee;

    @Column(name = "date_echeance", nullable = false)
    private LocalDate dateEcheance;

    @Column(name = "date_realisation")
    private LocalDate dateRealisation;

    @Column(nullable = false)
    @Builder.Default
    private Boolean obligatoire = true;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "badge_numero", length = 80)
    private String badgeNumero;

    @Column(name = "badge_date_remise")
    private LocalDate badgeDateRemise;
}
