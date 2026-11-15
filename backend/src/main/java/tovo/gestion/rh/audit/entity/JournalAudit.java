package tovo.gestion.rh.audit.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnTransformer;
import tovo.gestion.rh.utilisateur.entity.Utilisateur;
import java.time.LocalDateTime;

@Entity
@Table(name = "journal_audit")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JournalAudit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "date_heure", nullable = false, updatable = false) @Builder.Default private LocalDateTime dateHeure = LocalDateTime.now();
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "utilisateur_id") private Utilisateur utilisateur;
    @Column(length = 50) private String role;
    @Column(nullable = false, length = 20) private String action;
    @Column(nullable = false, length = 100) private String entite;
    @Column(name = "entite_id") private Long entiteId;
    @Column(name = "ancienne_valeur", columnDefinition = "jsonb")
    @ColumnTransformer(write = "to_jsonb(?)")
    private String ancienneValeur;
    @Column(name = "nouvelle_valeur", columnDefinition = "jsonb")
    @ColumnTransformer(write = "to_jsonb(?)")
    private String nouvelleValeur;
    @Column(name = "adresse_ip", columnDefinition = "inet")
    @ColumnTransformer(write = "?::inet")
    private String adresseIp;
}
