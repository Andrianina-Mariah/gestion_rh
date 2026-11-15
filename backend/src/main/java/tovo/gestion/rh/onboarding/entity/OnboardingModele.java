package tovo.gestion.rh.onboarding.entity;

import jakarta.persistence.*;
import lombok.*;
import tovo.gestion.rh.departement.entity.Departement;
import tovo.gestion.rh.poste.entity.Poste;

@Entity
@Table(name = "onboarding_modele")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OnboardingModele {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poste_id")
    private Poste poste;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departement_id")
    private Departement departement;

    @Column(nullable = false)
    @Builder.Default
    private Boolean actif = true;
}
