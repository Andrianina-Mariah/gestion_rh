package tovo.gestion.rh.onboarding.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tovo.gestion.rh.onboarding.entity.FormationAccueil;

import java.util.List;

public interface FormationAccueilRepository extends JpaRepository<FormationAccueil, Long> {
    List<FormationAccueil> findByEmployeIdOrderByDateFormationAsc(Long employeId);
    List<FormationAccueil> findByOnboardingIdOrderByDateFormationAsc(Long onboardingId);
}
