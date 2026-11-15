package tovo.gestion.rh.embauche.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "contrat")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contrat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employe_id", nullable = false)
    private Employe employe;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_contrat", nullable = false, length = 3)
    private TypeContratPropose typeContrat;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal salaire;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatutContrat statut = StatutContrat.BROUILLON;

    @Column(name = "contrat_url", length = 255)
    private String contratUrl;

    @Column(name = "date_creation", nullable = false)
    @Builder.Default
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "numero_renouvellement", nullable = false)
    @Builder.Default
    private Integer numeroRenouvellement = 0;

    @Column(length = 20)
    private String motif;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contrat_precedent_id")
    private Contrat contratPrecedent;
}
