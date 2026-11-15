package tovo.gestion.rh.workflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tovo.gestion.rh.workflow.entity.ValidationDemande;

import java.util.List;

public interface ValidationDemandeRepository extends JpaRepository<ValidationDemande, Integer> {

    List<ValidationDemande> findByDemandeIdOrderByDateValidationAscIdAsc(Integer demandeId);
}
