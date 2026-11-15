package tovo.gestion.rh.departement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tovo.gestion.rh.departement.entity.Departement;

import java.util.Optional;

public interface DepartementRepository extends JpaRepository<Departement, Integer> {

    boolean existsByNomIgnoreCase(String nom);

    boolean existsByNomIgnoreCaseAndIdNot(String nom, Integer id);

    Optional<Departement> findByNomIgnoreCase(String nom);
}