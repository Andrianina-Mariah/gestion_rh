package tovo.gestion.rh.utilisateur.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import tovo.gestion.rh.utilisateur.dto.AuthResponse;
import tovo.gestion.rh.utilisateur.dto.LoginRequest;
import tovo.gestion.rh.utilisateur.dto.ChangerMotDePasseRequest;
import tovo.gestion.rh.utilisateur.dto.UtilisateurResponse;
import tovo.gestion.rh.utilisateur.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UtilisateurResponse> me() {
        return ResponseEntity.ok(authService.getCurrentUser());
    }

    @PostMapping("/changer-mot-de-passe")
    public ResponseEntity<Void> changerMotDePasse(@Valid @RequestBody ChangerMotDePasseRequest request,
                                                   Authentication authentication) {
        authService.changerMotDePasse(authentication.getName(), request.ancienMotDePasse(), request.nouveauMotDePasse());
        return ResponseEntity.noContent().build();
    }
}
