package tovo.gestion.rh.candidature.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tovo.gestion.rh.candidature.entity.Evaluation;
import tovo.gestion.rh.utilisateur.entity.Utilisateur;

import java.time.LocalDateTime;
import java.util.List;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
    boolean existsByCandidatureIdAndType(Long candidatureId, tovo.gestion.rh.candidature.entity.TypeEvaluation type);
    List<Evaluation> findByCandidatureIdOrderByDatePlanifieeAsc(Long candidatureId);
    boolean existsByEvaluateurAndDatePlanifiee(Utilisateur evaluateur, LocalDateTime datePlanifiee);
}
