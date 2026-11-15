package tovo.gestion.rh.candidature.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cv_candidature")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CvCandidature {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidat_id", nullable = false)
    private Candidat candidat;

    @Column(name = "titre_profil", length = 150)
    private String titreProfil;

    @Column(columnDefinition = "TEXT")
    private String resume;

    @Column(columnDefinition = "TEXT")
    private String competences;

    @Column(length = 100)
    private String langues;

    @Column(name = "date_mise_a_jour", nullable = false)
    private LocalDateTime dateMiseAJour;

    @OneToMany(mappedBy = "cv", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExperienceProfessionnelle> experiences = new ArrayList<>();

    @OneToMany(mappedBy = "cv", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FormationCandidat> formations = new ArrayList<>();

    @PrePersist
    @PreUpdate
    void onChange() {
        dateMiseAJour = LocalDateTime.now();
    }
}
