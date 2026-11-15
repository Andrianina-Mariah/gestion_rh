package tovo.gestion.rh.utilisateur.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tovo.gestion.rh.utilisateur.entity.Utilisateur;

import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
    Optional<Utilisateur> findByEmail(String email);
    boolean existsByEmail(String email);
}