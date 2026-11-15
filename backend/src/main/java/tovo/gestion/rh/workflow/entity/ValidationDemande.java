package tovo.gestion.rh.workflow.entity;

import jakarta.persistence.*;
import lombok.*;
import tovo.gestion.rh.demanderecrutement.entity.DemandeRecrutement;
import tovo.gestion.rh.utilisateur.entity.Utilisateur;
import tovo.gestion.rh.workflow.DecisionValidation;
import tovo.gestion.rh.workflow.EtapeValidation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "validation_demande")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationDemande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "demande_recrutement_id", nullable = false)
    private DemandeRecrutement demande;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EtapeValidation etape;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "validateur_id", nullable = false)
    private Utilisateur validateur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DecisionValidation statut;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "budget_valide", precision = 12, scale = 2)
    private BigDecimal budgetValide;

    @Column(nullable = false)
    private Integer cycle;

    @Column(name = "date_validation", nullable = false, updatable = false)
    private LocalDateTime dateValidation;

    @PrePersist
    protected void onCreate() {
        if (dateValidation == null) {
            dateValidation = LocalDateTime.now();
        }
    }
}
