package tovo.gestion.rh.onboarding.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tovo.gestion.rh.onboarding.entity.Onboarding;
import tovo.gestion.rh.onboarding.entity.StatutOnboarding;

import java.util.List;
import java.util.Optional;

public interface OnboardingRepository extends JpaRepository<Onboarding, Long> {
    Optional<Onboarding> findByEmployeId(Long employeId);
    List<Onboarding> findAllByOrderByDateDebutDesc();
    List<Onboarding> findByStatutOrderByDateDebutDesc(StatutOnboarding statut);
}
