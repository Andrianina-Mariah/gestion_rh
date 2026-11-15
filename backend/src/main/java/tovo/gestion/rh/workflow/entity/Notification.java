package tovo.gestion.rh.workflow.entity;

import jakarta.persistence.*;
import lombok.*;
import tovo.gestion.rh.utilisateur.entity.Utilisateur;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destinataire_id", nullable = false)
    private Utilisateur destinataire;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(length = 255)
    private String lien;

    @Column(nullable = false)
    @Builder.Default
    private Boolean lue = false;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @PrePersist
    protected void onCreate() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
    }
}
