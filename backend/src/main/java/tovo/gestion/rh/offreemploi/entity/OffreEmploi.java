package tovo.gestion.rh.offreemploi.entity;

import jakarta.persistence.*;
import lombok.*;
import tovo.gestion.rh.demanderecrutement.entity.DemandeRecrutement;

import java.time.LocalDateTime;

@Entity
@Table(name = "offre_emploi")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OffreEmploi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "demande_recrutement_id", nullable = false)
    private DemandeRecrutement demandeRecrutement;

    @Column(name = "titre_offre", nullable = false, length = 200)
    private String titreOffre;

    @Column(name = "description_poste", nullable = false, columnDefinition = "TEXT")
    private String descriptionPoste;

    @Column(name = "profil_recherche", nullable = false, columnDefinition = "TEXT")
    private String profilRecherche;

    @Column(name = "lieu_travail", length = 100)
    private String lieuTravail;

    @Column(name = "type_contrat_propose", length = 50)
    private String typeContratPropose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private StatutOffre statut = StatutOffre.BROUILLON;

    @Column(name = "date_publication")
    private LocalDateTime datePublication;

    @Column(name = "date_cloture")
    private LocalDateTime dateCloture;
}
