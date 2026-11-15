package tovo.gestion.rh.candidature.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "experience_professionnelle")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExperienceProfessionnelle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cv_candidature_id", nullable = false)
    private CvCandidature cv;

    @Column(nullable = false, length = 150)
    private String poste;

    @Column(nullable = false, length = 150)
    private String entreprise;

    private LocalDate dateDebut;
    private LocalDate dateFin;

    @Column(nullable = false)
    private boolean enPoste;

    @Column(columnDefinition = "TEXT")
    private String description;
}
