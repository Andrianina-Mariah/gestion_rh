package tovo.gestion.rh.candidature.entity;

import jakarta.persistence.*;
import lombok.*;
import tovo.gestion.rh.utilisateur.entity.Utilisateur;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "evaluation")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Evaluation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidature_id", nullable = false)
    private Candidature candidature;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_evaluation", nullable = false, length = 20)
    private TypeEvaluation type;

    @Column(name = "date_planifiee", nullable = false)
    private LocalDateTime datePlanifiee;

    @Column(name = "date_realisation")
    private LocalDateTime dateRealisation;

    @Column(precision = 5, scale = 2)
    private BigDecimal note;

    @Column(columnDefinition = "TEXT")
    private String remarque;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ResultatEvaluation resultat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluateur_id")
    private Utilisateur evaluateur;
}
