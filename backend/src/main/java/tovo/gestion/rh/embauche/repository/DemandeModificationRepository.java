package tovo.gestion.rh.embauche.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tovo.gestion.rh.embauche.entity.DemandeModification;
import tovo.gestion.rh.embauche.entity.StatutDemandeModification;
import java.util.List;

public interface DemandeModificationRepository extends JpaRepository<DemandeModification, Long> {
    List<DemandeModification> findByStatutOrderByDateDemandeAsc(StatutDemandeModification statut);
    List<DemandeModification> findByEmployeIdOrderByDateDemandeDesc(Long employeId);
}
