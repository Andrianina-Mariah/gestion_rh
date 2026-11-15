package tovo.gestion.rh.candidature.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tovo.gestion.rh.candidature.entity.CvCandidature;

public interface CvCandidatureRepository extends JpaRepository<CvCandidature, Long> {
}
