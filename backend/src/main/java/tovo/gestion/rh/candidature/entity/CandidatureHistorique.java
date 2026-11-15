package tovo.gestion.rh.candidature.entity;

import jakarta.persistence.*;
import lombok.*;
import tovo.gestion.rh.utilisateur.entity.Utilisateur;

import java.time.LocalDateTime;

@Entity
@Table(name = "candidature_historique")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidatureHistorique {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidature_id", nullable = false)
    private Candidature candidature;

    @Enumerated(EnumType.STRING)
    @Column(name = "ancien_statut", length = 50)
    private StatutCandidature ancienStatut;

    @Enumerated(EnumType.STRING)
    @Column(name = "nouveau_statut", nullable = false, length = 50)
    private StatutCandidature nouveauStatut;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    private String commentaire;

    @Column(name = "date", nullable = false)
    private LocalDateTime date;
}
