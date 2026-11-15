package tovo.gestion.rh.embauche.entity;

import jakarta.persistence.*;
import lombok.*;
import tovo.gestion.rh.candidature.entity.Candidature;
import tovo.gestion.rh.utilisateur.entity.Utilisateur;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "proposition_embauche")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropositionEmbauche {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "candidature_id", nullable = false)
    private Candidature candidature;

    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposition_precedente_id")
    private PropositionEmbauche propositionPrecedente;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "emise_par_id", nullable = false)
    private Utilisateur emisePar;

    @Column(name = "date_travail_souhaitee", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "lieu_travail", nullable = false, length = 150)
    private String lieu;

    @Column(name = "salaire_propose", nullable = false, precision = 12, scale = 2)
    private BigDecimal salaire;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_contrat_propose", nullable = false, length = 3)
    private TypeContratPropose typeContrat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private StatutProposition statut = StatutProposition.EN_ATTENTE;

    @Column(name = "remarques_candidat", columnDefinition = "TEXT")
    private String remarqueCandidat;

    @Column(name = "souhaits_candidat", columnDefinition = "TEXT")
    private String souhaitsCandidat;

    @Column(name = "date_proposition", nullable = false)
    @Builder.Default
    private LocalDateTime dateProposition = LocalDateTime.now();

    @Column(name = "date_reponse")
    private LocalDateTime dateReponse;

    @Column(name = "jeton_acces_hash", length = 64)
    private String jetonAccesHash;

    @Column(name = "jeton_expire_le")
    private LocalDateTime jetonExpireLe;
}
