package tovo.gestion.rh.embauche.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tovo.gestion.rh.embauche.entity.ContactUrgence;
import java.util.List;

public interface ContactUrgenceRepository extends JpaRepository<ContactUrgence, Long> {
    List<ContactUrgence> findByEmployeIdOrderByPrincipalDescIdAsc(Long employeId);
}
