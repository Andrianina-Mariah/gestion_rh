package tovo.gestion.rh.offreemploi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tovo.gestion.rh.offreemploi.entity.OffreEmploi;
import tovo.gestion.rh.offreemploi.entity.StatutOffre;

import java.util.List;
import java.util.Optional;

public interface OffreEmploiRepository extends JpaRepository<OffreEmploi, Long> {

    List<OffreEmploi> findAllByOrderByIdDesc();

    List<OffreEmploi> findByStatutOrderByDatePublicationDesc(StatutOffre statut);

    Optional<OffreEmploi> findFirstByDemandeRecrutementIdOrderByIdAsc(Integer demandeId);
    List<OffreEmploi> findByDemandeRecrutementDemandeurIdOrderByIdDesc(Integer demandeurId);
}
