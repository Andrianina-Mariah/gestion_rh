package tovo.gestion.rh.onboarding.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tovo.gestion.rh.onboarding.entity.OnboardingModele;

import java.util.Optional;

public interface OnboardingModeleRepository extends JpaRepository<OnboardingModele, Long> {
    Optional<OnboardingModele> findFirstByPosteIdAndActifTrue(Integer posteId);
    Optional<OnboardingModele> findFirstByDepartementIdAndActifTrue(Integer departementId);
    Optional<OnboardingModele> findFirstByPosteIsNullAndDepartementIsNullAndActifTrue();
}
