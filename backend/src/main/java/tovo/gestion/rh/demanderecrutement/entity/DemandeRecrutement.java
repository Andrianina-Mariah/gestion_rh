package tovo.gestion.rh.demanderecrutement.entity;

import jakarta.persistence.*;
import lombok.*;
import tovo.gestion.rh.poste.entity.Poste;
import tovo.gestion.rh.utilisateur.entity.Utilisateur;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "demande_recrutement")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandeRecrutement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "poste_id", nullable = false)
    private Poste poste;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "demandeur_id", nullable = false)
    private Utilisateur demandeur;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "validateur_id")
    private Utilisateur validateur;

    @Column(name = "nombre_postes", nullable = false)
    private Integer nombrePostes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TypeDemande type;

    @Column(columnDefinition = "TEXT")
    private String motif;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal budget;

    @Column(name = "date_souhaitee", nullable = false)
    private LocalDate dateSouhaitee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private StatutDemande statut;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    @Column(name = "employe_remplace_id")
    private Integer employeRemplaceId;

    @Column(nullable = false)
    @Builder.Default
    private Integer cycle = 1;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Integer version = 0;

    @Column(name = "date_ouverture_recrutement")
    private LocalDateTime dateOuvertureRecrutement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ouvert_par_id")
    private Utilisateur ouvertPar;

    @PrePersist
    protected void onCreate() {
        this.dateCreation = LocalDateTime.now();
        if (this.statut == null) {
            this.statut = StatutDemande.BROUILLON;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.dateModification = LocalDateTime.now();
    }
}