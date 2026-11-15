package tovo.gestion.rh.embauche.entity;

import jakarta.persistence.*;
import lombok.*;
import tovo.gestion.rh.utilisateur.entity.Utilisateur;
import java.time.LocalDateTime;

@Entity
@Table(name = "demande_modification")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DemandeModification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "employe_id") private Employe employe;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "demandeur_id") private Utilisateur demandeur;
    @Column(nullable = false, length = 80) private String champ;
    @Column(name = "ancienne_valeur", columnDefinition = "TEXT") private String ancienneValeur;
    @Column(name = "nouvelle_valeur", nullable = false, columnDefinition = "TEXT") private String nouvelleValeur;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) @Builder.Default private StatutDemandeModification statut = StatutDemandeModification.EN_ATTENTE;
    @Column(columnDefinition = "TEXT") private String commentaire;
    @Column(name = "date_demande", nullable = false) @Builder.Default private LocalDateTime dateDemande = LocalDateTime.now();
    @Column(name = "date_decision") private LocalDateTime dateDecision;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "decideur_id") private Utilisateur decideur;
}
