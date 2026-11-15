package tovo.gestion.rh.embauche.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tovo.gestion.rh.embauche.entity.EmployeAdresse;
import java.util.List;

public interface EmployeAdresseRepository extends JpaRepository<EmployeAdresse, Long> {
    List<EmployeAdresse> findByEmployeIdOrderByDateDebutDesc(Long employeId);
}
