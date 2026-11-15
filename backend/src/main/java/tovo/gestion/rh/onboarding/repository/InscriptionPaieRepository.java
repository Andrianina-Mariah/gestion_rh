package tovo.gestion.rh.onboarding.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tovo.gestion.rh.onboarding.entity.InscriptionPaie;

import java.util.Optional;

public interface InscriptionPaieRepository extends JpaRepository<InscriptionPaie, Long> {
    Optional<InscriptionPaie> findByEmployeId(Long employeId);
}
