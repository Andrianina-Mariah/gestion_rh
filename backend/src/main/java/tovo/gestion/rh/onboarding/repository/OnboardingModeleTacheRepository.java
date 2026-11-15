package tovo.gestion.rh.onboarding.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tovo.gestion.rh.onboarding.entity.OnboardingModeleTache;

import java.util.List;

public interface OnboardingModeleTacheRepository extends JpaRepository<OnboardingModeleTache, Long> {
    List<OnboardingModeleTache> findByModeleIdOrderByIdAsc(Long modeleId);
}
