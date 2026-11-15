package tovo.gestion.rh.embauche.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tovo.gestion.rh.embauche.entity.EmployeContact;
import java.util.List;

public interface EmployeContactRepository extends JpaRepository<EmployeContact, Long> {
    List<EmployeContact> findByEmployeIdOrderByDateDebutDesc(Long employeId);
}
