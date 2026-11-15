package tovo.gestion.rh.candidature.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tovo.gestion.rh.candidature.entity.Candidature;
import tovo.gestion.rh.candidature.entity.StatutCandidature;

import java.util.Optional;
import java.util.List;

public interface CandidatureRepository extends JpaRepository<Candidature, Long> {
    boolean existsByOffreIdAndCandidatId(Long offreId, Long candidatId);
    Page<Candidature> findByOffreId(Long offreId, Pageable pageable);
    Page<Candidature> findByOffreIdAndStatut(Long offreId, StatutCandidature statut, Pageable pageable);
    Optional<Candidature> findByIdAndOffreId(Long id, Long offreId);
    List<Candidature> findByOffreIdAndStatutOrderByRangAttenteAscDateCandidatureAsc(
            Long offreId, StatutCandidature statut);
    @Query("select max(c.rangAttente) from Candidature c " +
            "where c.offre.id = :offreId and c.statut = :statut")
    Integer findMaxRangAttente(@Param("offreId") Long offreId,
                               @Param("statut") StatutCandidature statut);
    long countByOffreIdAndStatut(Long offreId, StatutCandidature statut);
    List<Candidature> findByOffreIdAndStatutIn(Long offreId, List<StatutCandidature> statuts);
}
