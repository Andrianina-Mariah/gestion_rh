package tovo.gestion.rh.embauche.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tovo.gestion.rh.embauche.entity.Affectation;

import java.util.List;
import java.util.Optional;

public interface AffectationRepository extends JpaRepository<Affectation, Long> {
    List<Affectation> findByEmployeIdOrderByDateAffectationDesc(Long employeId);
    Optional<Affectation> findFirstByEmployeIdAndDateFinIsNullOrderByDateAffectationDesc(Long employeId);
}
