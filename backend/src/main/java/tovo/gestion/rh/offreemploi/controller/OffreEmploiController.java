package tovo.gestion.rh.offreemploi.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tovo.gestion.rh.offreemploi.dto.OffreEmploiRequest;
import tovo.gestion.rh.offreemploi.dto.OffreEmploiResponse;
import tovo.gestion.rh.offreemploi.service.OffreEmploiService;
import tovo.gestion.rh.utilisateur.entity.Utilisateur;
import org.springframework.security.core.Authentication;
import tovo.gestion.rh.utilisateur.repository.UtilisateurRepository;

import java.util.List;

@RestController
@RequestMapping("/api/offres")
@RequiredArgsConstructor
public class OffreEmploiController {

    private final OffreEmploiService offreService;
    private final UtilisateurRepository utilisateurRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'MANAGER')")
    public ResponseEntity<List<OffreEmploiResponse>> findAll(Authentication authentication) {
        return ResponseEntity.ok(offreService.findAll(currentUser(authentication)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'MANAGER')")
    public ResponseEntity<OffreEmploiResponse> findById(
            @PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(offreService.findById(id, currentUser(authentication)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<OffreEmploiResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody OffreEmploiRequest request,
            @AuthenticationPrincipal Utilisateur rh) {
        return ResponseEntity.ok(offreService.update(id, request, rh));
    }

    @PostMapping("/{id}/publier")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<OffreEmploiResponse> publier(
            @PathVariable Long id,
            @AuthenticationPrincipal Utilisateur rh) {
        return ResponseEntity.ok(offreService.publier(id, rh));
    }

    @PostMapping("/{id}/cloturer")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<OffreEmploiResponse> cloturer(
            @PathVariable Long id,
            @AuthenticationPrincipal Utilisateur rh) {
        return ResponseEntity.ok(offreService.cloturer(id, rh));
    }

    private Utilisateur currentUser(Authentication authentication) {
        return utilisateurRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Utilisateur authentifié introuvable"));
    }
}
