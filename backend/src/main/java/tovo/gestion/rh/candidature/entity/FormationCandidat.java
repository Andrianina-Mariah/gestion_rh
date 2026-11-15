package tovo.gestion.rh.candidature.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "formation_candidat")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormationCandidat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cv_candidature_id", nullable = false)
    private CvCandidature cv;

    @Column(nullable = false, length = 150)
    private String diplome;

    @Column(nullable = false, length = 150)
    private String etablissement;

    private Integer anneeObtention;

    @Column(columnDefinition = "TEXT")
    private String description;
}
