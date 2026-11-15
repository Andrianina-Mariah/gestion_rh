package tovo.gestion.rh.onboarding.entity;

import jakarta.persistence.*;
import lombok.*;
import tovo.gestion.rh.embauche.entity.Employe;
import tovo.gestion.rh.utilisateur.entity.Utilisateur;
import tovo.gestion.rh.embauche.entity.TypeDocument;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "document")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentEmploye {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employe_id", nullable = false)
    private Employe employe;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "onboarding_tache_id")
    private OnboardingTache onboardingTache;
    @Column(nullable = false, length = 30)
    private String type;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_document_id")
    private TypeDocument typeDocument;
    @Column(name = "nom_fichier", nullable = false, length = 255)
    private String nomFichier;
    @Column(nullable = false, length = 500)
    private String url;
    @Column(nullable = false)
    private Long taille;
    @Column(name = "date_depot", nullable = false)
    private LocalDateTime dateDepot;
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String statut = "DEPOSE";
    @Column(name = "date_emission")
    private LocalDate dateEmission;
    @Column(name = "date_expiration")
    private LocalDate dateExpiration;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valide_par_id")
    private Utilisateur validePar;
    @Column(name = "date_validation")
    private LocalDateTime dateValidation;
    @Column(name = "motif_rejet", columnDefinition = "TEXT")
    private String motifRejet;
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String confidentialite = "INTERNE";
    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "depose_par_id", nullable = false)
    private Utilisateur deposePar;
}
