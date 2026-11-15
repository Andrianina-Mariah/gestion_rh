package tovo.gestion.rh.embauche.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tovo.gestion.rh.embauche.entity.AyantDroit;
import java.util.List;

public interface AyantDroitRepository extends JpaRepository<AyantDroit, Long> {
    List<AyantDroit> findByEmployeIdOrderByDateNaissanceAsc(Long employeId);
}
