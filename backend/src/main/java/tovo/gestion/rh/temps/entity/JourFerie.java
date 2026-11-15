package tovo.gestion.rh.temps.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity @Table(name = "jour_ferie") @Getter @Setter @NoArgsConstructor
public class JourFerie {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true) private LocalDate date;
    @Column(nullable = false, length = 150) private String libelle;
    @Column(nullable = false) private Boolean paye;
}
