package tovo.gestion.rh.utilisateur.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tovo.gestion.rh.utilisateur.entity.Role;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByCode(String code);
}