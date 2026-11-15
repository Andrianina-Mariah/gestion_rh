package tovo.gestion.rh.candidature.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tovo.gestion.rh.candidature.entity.Candidat;

import java.util.Optional;

public interface CandidatRepository extends JpaRepository<Candidat, Long> {
    Optional<Candidat> findByEmailIgnoreCase(String email);
}
