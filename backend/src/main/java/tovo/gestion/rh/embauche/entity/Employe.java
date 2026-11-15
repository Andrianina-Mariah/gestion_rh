package tovo.gestion.rh.embauche.entity;

import jakarta.persistence.*;
import lombok.*;
import tovo.gestion.rh.candidature.entity.Candidat;

import java.time.LocalDate;

@Entity
@Table(name = "employe")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employe {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "candidat_id", nullable = false, unique = true)
    private Candidat candidat;

    @Column(nullable = false, unique = true, length = 50, insertable = false, updatable = false)
    private String matricule;

    @Column(name = "email_professionnel", length = 150)
    private String emailProfessionnel;

    @Column(name = "telephone_professionnel", length = 30)
    private String telephoneProfessionnel;

    @Column(name = "date_embauche", nullable = false)
    private LocalDate dateEmbauche;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private StatutEmploye statut = StatutEmploye.EN_ONBOARDING;

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;
    @Column(name = "lieu_naissance", length = 150)
    private String lieuNaissance;
    @Column(length = 20)
    private String sexe;
    @Column(length = 80)
    private String nationalite;
    @Column(length = 50)
    private String cin;
    @Column(name = "numero_cnaps", length = 50)
    private String numeroCnaps;
    @Column(name = "situation_familiale", length = 30)
    private String situationFamiliale;
    @Column(name = "nombre_enfants", nullable = false)
    @Builder.Default
    private Integer nombreEnfants = 0;
    @Column(name = "date_sortie")
    private LocalDate dateSortie;
    @Column(name = "motif_sortie", length = 30)
    private String motifSortie;
}
