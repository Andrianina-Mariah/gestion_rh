package tovo.gestion.rh.candidature.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tovo.gestion.rh.offreemploi.entity.OffreEmploi;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "candidature")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Candidature {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "offre_emploi_id", nullable = false)
    private OffreEmploi offre;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "candidat_id", nullable = false)
    private Candidat candidat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cv_candidature_id")
    private CvCandidature cv;

    @Column(name = "cv_url", length = 255)
    private String cvUrl;

    @Column(name = "lettre_motivation_url", length = 255)
    private String lettreMotivationUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cv_snapshot", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> cvSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private StatutCandidature statut = StatutCandidature.RECU;

    @Column(name = "rang_attente")
    private Integer rangAttente;

    @Column(name = "date_candidature", nullable = false)
    private LocalDateTime dateCandidature;

    @PrePersist
    void onCreate() {
        dateCandidature = LocalDateTime.now();
    }
}
