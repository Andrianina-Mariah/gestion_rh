package tovo.gestion.rh.onboarding.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tovo.gestion.rh.onboarding.entity.OnboardingTache;

import java.util.List;

public interface OnboardingTacheRepository extends JpaRepository<OnboardingTache, Long> {
    List<OnboardingTache> findByOnboardingIdOrderByDateEcheanceAscIdAsc(Long onboardingId);
}
