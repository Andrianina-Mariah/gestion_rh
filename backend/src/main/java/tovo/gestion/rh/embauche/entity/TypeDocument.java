package tovo.gestion.rh.embauche.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "type_document")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TypeDocument {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 40) private String code;
    @Column(nullable = false, length = 150) private String libelle;
    @Column(nullable = false) @Builder.Default private Boolean obligatoire = false;
    @Column(name = "duree_validite_mois") private Integer dureeValiditeMois;
    @Column(nullable = false, length = 20) @Builder.Default private String confidentialite = "INTERNE";
}
