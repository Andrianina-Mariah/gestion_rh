package tovo.gestion.rh.embauche.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tovo.gestion.rh.embauche.entity.Contrat;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

public interface ContratRepository extends JpaRepository<Contrat, Long> {
    List<Contrat> findByEmployeIdOrderByDateDebutAsc(Long employeId);
    Optional<Contrat> findFirstByEmployeIdOrderByDateCreationDesc(Long employeId);
    List<Contrat> findByDateFinIsNotNullAndDateFinBetweenOrderByDateFinAsc(
            LocalDate from, LocalDate to);
}
