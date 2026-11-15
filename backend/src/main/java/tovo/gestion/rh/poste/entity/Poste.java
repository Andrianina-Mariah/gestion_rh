package tovo.gestion.rh.poste.entity;

import jakarta.persistence.*;
import lombok.*;
import tovo.gestion.rh.departement.entity.Departement;

@Entity
@Table(name = "poste")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Poste {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 150)
    private String titre;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "departement_id", nullable = false)
    private Departement departement;

    @Column(name = "description_profil", columnDefinition = "TEXT")
    private String descriptionProfil;
}