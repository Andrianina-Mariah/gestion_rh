package tovo.gestion.rh.utilisateur.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import tovo.gestion.rh.security.JwtService;
import tovo.gestion.rh.utilisateur.dto.AuthResponse;
import tovo.gestion.rh.utilisateur.dto.LoginRequest;
import tovo.gestion.rh.utilisateur.dto.UtilisateurResponse;
import tovo.gestion.rh.utilisateur.entity.Utilisateur;
import tovo.gestion.rh.utilisateur.repository.UtilisateurRepository;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        Utilisateur utilisateur = (Utilisateur) authentication.getPrincipal();
        String token = jwtService.generateToken(utilisateur);

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .id(utilisateur.getId())
                .email(utilisateur.getEmail())
                .nom(utilisateur.getNom())
                .prenom(utilisateur.getPrenom())
                .role(utilisateur.getRole().getCode())
                .changementMotDePasseRequis(utilisateur.getChangementMotDePasseRequis())
                .build();
    }

    @Transactional
    public void changerMotDePasse(String email, String ancienMotDePasse, String nouveauMotDePasse) {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Utilisateur introuvable"));
        if (!passwordEncoder.matches(ancienMotDePasse, utilisateur.getMotDePasse())) {
            throw new IllegalArgumentException("Le mot de passe actuel est incorrect");
        }
        if (passwordEncoder.matches(nouveauMotDePasse, utilisateur.getMotDePasse())) {
            throw new IllegalArgumentException("Choisissez un nouveau mot de passe différent de l'actuel");
        }
        utilisateur.setMotDePasse(passwordEncoder.encode(nouveauMotDePasse));
        utilisateur.setChangementMotDePasseRequis(false);
        utilisateurRepository.save(utilisateur);
    }

    public UtilisateurResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Utilisateur non authentifié");
        }

        Object principal = authentication.getPrincipal();
        Utilisateur utilisateur;

        if (principal instanceof Utilisateur) {
            utilisateur = (Utilisateur) principal;
        } else {
            String email = authentication.getName();
            utilisateur = utilisateurRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        }

        return UtilisateurResponse.builder()
                .id(utilisateur.getId())
                .nom(utilisateur.getNom())
                .prenom(utilisateur.getPrenom())
                .email(utilisateur.getEmail())
                .role(utilisateur.getRole().getCode())
                .actif(utilisateur.getActif())
                .changementMotDePasseRequis(utilisateur.getChangementMotDePasseRequis())
                .build();
    }
}
