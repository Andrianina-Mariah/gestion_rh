package tovo.gestion.rh.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import tovo.gestion.rh.utilisateur.entity.Utilisateur;
import tovo.gestion.rh.utilisateur.repository.UtilisateurRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        List<Utilisateur> users = utilisateurRepository.findAll();
        for (Utilisateur user : users) {
            // Ne re-hash que si ce n'est pas déjà un hash BCrypt
            if (user.getMotDePasse() != null && !user.getMotDePasse().startsWith("$2a$")) {
                user.setMotDePasse(passwordEncoder.encode("password"));
                utilisateurRepository.save(user);
            }
        }
        System.out.println(">>> Mots de passe vérifiés / mis à jour (password)");
    }
}