package tovo.gestion.rh.embauche.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "contact_urgence")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ContactUrgence {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "employe_id") private Employe employe;
    @Column(nullable = false, length = 150) private String nom;
    @Column(name = "lien_parente", nullable = false, length = 80) private String lienParente;
    @Column(nullable = false, length = 30) private String telephone;
    @Column(nullable = false) @Builder.Default private Boolean principal = false;
}
