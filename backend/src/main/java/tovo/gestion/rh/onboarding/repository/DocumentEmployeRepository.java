package tovo.gestion.rh.onboarding.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tovo.gestion.rh.onboarding.entity.DocumentEmploye;

import java.util.List;
import java.time.LocalDate;

public interface DocumentEmployeRepository extends JpaRepository<DocumentEmploye, Long> {
    List<DocumentEmploye> findByEmployeIdOrderByDateDepotDesc(Long employeId);
    List<DocumentEmploye> findByEmployeIdAndTypeOrderByVersionDesc(Long employeId, String type);
    List<DocumentEmploye> findByDateExpirationBetweenAndStatut(LocalDate from, LocalDate to, String statut);
    List<DocumentEmploye> findByDateExpirationBeforeAndStatut(LocalDate date, String statut);
}
