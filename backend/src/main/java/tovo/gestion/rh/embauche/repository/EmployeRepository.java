package tovo.gestion.rh.embauche.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tovo.gestion.rh.embauche.entity.Employe;
import tovo.gestion.rh.embauche.entity.StatutEmploye;

import java.util.List;
import java.util.Optional;

public interface EmployeRepository extends JpaRepository<Employe, Long> {
    Optional<Employe> findByCandidatId(Long candidatId);
    List<Employe> findAllByOrderByIdDesc();
    List<Employe> findByStatutOrderByIdDesc(StatutEmploye statut);
}
