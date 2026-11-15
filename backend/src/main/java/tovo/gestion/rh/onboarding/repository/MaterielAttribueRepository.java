package tovo.gestion.rh.onboarding.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tovo.gestion.rh.onboarding.entity.MaterielAttribue;

import java.util.List;

public interface MaterielAttribueRepository extends JpaRepository<MaterielAttribue, Long> {
    List<MaterielAttribue> findByEmployeIdOrderByDateAttributionDesc(Long employeId);
    boolean existsByNumeroSerie(String numeroSerie);
}
