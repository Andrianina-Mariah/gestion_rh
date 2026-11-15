package tovo.gestion.rh.embauche.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tovo.gestion.rh.embauche.entity.HistoriqueSalaire;
import java.util.List;

public interface HistoriqueSalaireRepository extends JpaRepository<HistoriqueSalaire, Long> {
    List<HistoriqueSalaire> findByEmployeIdOrderByDateDebutDesc(Long employeId);
}
