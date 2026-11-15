package tovo.gestion.rh.candidature.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "candidat")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Candidat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, length = 100)
    private String prenom;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(length = 30)
    private String telephone;

    @Column(columnDefinition = "TEXT")
    private String adresse;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @PrePersist
    void onCreate() {
        dateCreation = LocalDateTime.now();
    }
}
