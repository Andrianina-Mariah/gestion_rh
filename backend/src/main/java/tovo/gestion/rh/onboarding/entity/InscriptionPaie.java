package tovo.gestion.rh.onboarding.entity;

import jakarta.persistence.*;
import lombok.*;
import tovo.gestion.rh.embauche.entity.Employe;

import java.time.LocalDate;

@Entity
@Table(name = "inscription_paie")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InscriptionPaie {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employe_id", nullable = false, unique = true)
    private Employe employe;
    @Column(name = "date_inscription")
    private LocalDate dateInscription;
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String statut = "A_FAIRE";
    @Column(name = "reference_paie", length = 100)
    private String referencePaie;
    @Column(name = "mode_paiement", length = 30)
    private String modePaiement;
    @Column(name = "numero_compte", length = 100)
    private String numeroCompte;
}
