package tovo.gestion.rh.embauche.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tovo.gestion.rh.embauche.dto.*;
import tovo.gestion.rh.embauche.service.PropositionEmbaucheService;
import tovo.gestion.rh.utilisateur.entity.Utilisateur;
import tovo.gestion.rh.utilisateur.repository.UtilisateurRepository;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PropositionEmbaucheController {
    private final PropositionEmbaucheService propositionService;
    private final UtilisateurRepository utilisateurRepository;

    @PostMapping("/api/candidatures/{id}/propositions")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    public ResponseEntity<PropositionResponse> creer(
            @PathVariable Long id,
            @Valid @RequestBody CreerPropositionRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(propositionService.creer(id, request, currentUser(authentication)));
    }

    @PostMapping("/api/propositions/{id}/repondre")
    public ResponseEntity<PropositionResponse> repondre(
            @PathVariable Long id,
            @Valid @RequestBody RepondrePropositionRequest request) {
        return ResponseEntity.ok(propositionService.repondre(id, request));
    }

    @GetMapping("/api/public/propositions/{id}")
    public ResponseEntity<PropositionResponse> consulterPubliquement(
            @PathVariable Long id,
            @RequestParam String jeton) {
        return ResponseEntity.ok(propositionService.consulterPubliquement(id, jeton));
    }

    @GetMapping("/api/public/propositions/{id}/versions")
    public ResponseEntity<List<PropositionResponse>> historiquePublic(
            @PathVariable Long id,
            @RequestParam String jeton) {
        return ResponseEntity.ok(propositionService.historiquePublique(id, jeton));
    }

    @PostMapping("/api/propositions/{id}/ajuster")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    public ResponseEntity<PropositionResponse> ajuster(
            @PathVariable Long id,
            @Valid @RequestBody AjusterPropositionRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(propositionService.ajuster(
                id, request, currentUser(authentication)));
    }

    @PostMapping("/api/propositions/{id}/rejeter")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    public ResponseEntity<PropositionResponse> rejeter(
            @PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(propositionService.rejeter(id, currentUser(authentication)));
    }

    @GetMapping("/api/candidatures/{id}/propositions")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'MANAGER')")
    public ResponseEntity<List<PropositionResponse>> historique(
            @PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(propositionService.historique(id, currentUser(authentication)));
    }

    private Utilisateur currentUser(Authentication authentication) {
        return utilisateurRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Utilisateur authentifié introuvable"));
    }
}
