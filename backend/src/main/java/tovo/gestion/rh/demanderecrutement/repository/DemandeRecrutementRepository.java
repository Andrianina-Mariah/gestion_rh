package tovo.gestion.rh.demanderecrutement.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tovo.gestion.rh.demanderecrutement.entity.DemandeRecrutement;
import tovo.gestion.rh.demanderecrutement.entity.StatutDemande;

import java.util.List;

public interface DemandeRecrutementRepository extends JpaRepository<DemandeRecrutement, Integer> {

    Page<DemandeRecrutement> findByDemandeurId(Integer demandeurId, Pageable pageable);

    Page<DemandeRecrutement> findByStatut(StatutDemande statut, Pageable pageable);

    @Query("""
        SELECT d FROM DemandeRecrutement d
        WHERE (:statut IS NULL OR d.statut = :statut)
          AND (:demandeurId IS NULL OR d.demandeur.id = :demandeurId)
          AND (:departementId IS NULL OR d.poste.departement.id = :departementId)
        """)
    Page<DemandeRecrutement> search(
            @Param("statut") StatutDemande statut,
            @Param("demandeurId") Integer demandeurId,
            @Param("departementId") Integer departementId,
            Pageable pageable
    );

    boolean existsByPosteId(Integer posteId);

    List<DemandeRecrutement> findByStatutOrderByDateCreationAsc(StatutDemande statut);
}