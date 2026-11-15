package tovo.gestion.rh.embauche.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tovo.gestion.rh.embauche.entity.PropositionEmbauche;

import java.util.List;
import java.util.Optional;

public interface PropositionEmbaucheRepository extends JpaRepository<PropositionEmbauche, Long> {
    List<PropositionEmbauche> findByCandidatureIdOrderByVersionAsc(Long candidatureId);
    Optional<PropositionEmbauche> findByCandidatureIdAndStatutIn(
            Long candidatureId, List<tovo.gestion.rh.embauche.entity.StatutProposition> statuts);
}
