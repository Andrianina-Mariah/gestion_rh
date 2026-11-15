package tovo.gestion.rh.poste.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tovo.gestion.rh.poste.entity.Poste;

import java.util.List;

public interface PosteRepository extends JpaRepository<Poste, Integer> {

    List<Poste> findByDepartementId(Integer departementId);

    boolean existsByDepartementId(Integer departementId);
}