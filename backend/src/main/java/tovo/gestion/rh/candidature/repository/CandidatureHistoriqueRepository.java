package tovo.gestion.rh.candidature.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tovo.gestion.rh.candidature.entity.CandidatureHistorique;

import java.util.List;

public interface CandidatureHistoriqueRepository extends JpaRepository<CandidatureHistorique, Long> {
    List<CandidatureHistorique> findByCandidatureIdOrderByDateAsc(Long candidatureId);
}
