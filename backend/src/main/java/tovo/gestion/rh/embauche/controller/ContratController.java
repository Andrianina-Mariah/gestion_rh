package tovo.gestion.rh.embauche.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tovo.gestion.rh.embauche.dto.ContratResponse;
import tovo.gestion.rh.embauche.dto.AccesEmployeResponse;
import tovo.gestion.rh.embauche.dto.SignerContratRequest;
import tovo.gestion.rh.embauche.dto.ValiderContratRequest;
import tovo.gestion.rh.embauche.service.ContratService;
import tovo.gestion.rh.utilisateur.entity.Utilisateur;
import tovo.gestion.rh.utilisateur.repository.UtilisateurRepository;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ContratController {

    private final ContratService contratService;
    private final UtilisateurRepository utilisateurRepository;

    @GetMapping("/api/employes/{id}/contrats")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'MANAGER', 'EMPLOYE')")
    public ResponseEntity<List<ContratResponse>> findByEmploye(
            @PathVariable Long id,
            Authentication authentication) {
        currentUser(authentication);
        return ResponseEntity.ok(contratService.findByEmployeId(id));
    }

    @GetMapping("/api/contrats/{id}")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'MANAGER', 'EMPLOYE')")
    public ResponseEntity<ContratResponse> findById(
            @PathVariable Long id,
            Authentication authentication) {
        currentUser(authentication);
        return ResponseEntity.ok(contratService.findById(id));
    }

    @PostMapping("/api/contrats/{id}/signer")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    public ResponseEntity<ContratResponse> signer(
            @PathVariable Long id,
            Authentication authentication,
            @RequestBody(required = false) SignerContratRequest request) {
        currentUser(authentication);
        return ResponseEntity.ok(contratService.signer(id, request));
    }

    @PostMapping("/api/contrats/{id}/valider")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    public ResponseEntity<ContratResponse> valider(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ValiderContratRequest request,
            Authentication authentication) {
        Utilisateur utilisateur = currentUser(authentication);
        return ResponseEntity.ok(contratService.valider(id, request, utilisateur));
    }

    @PostMapping("/api/contrats/{id}/reinitialiser-acces")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    public ResponseEntity<AccesEmployeResponse> reinitialiserAcces(
            @PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(contratService.reinitialiserAcces(id, currentUser(authentication)));
    }

    @PostMapping("/api/contrats/{id}/renouveler")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    public ResponseEntity<ContratResponse> renouveler(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(contratService.renouveler(id, currentUser(authentication)));
    }

    @PostMapping("/api/contrats/{id}/convertir-cdi")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    public ResponseEntity<ContratResponse> convertirCdi(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(contratService.convertirCdi(id, currentUser(authentication)));
    }

    @GetMapping("/api/contrats/echeances")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    public ResponseEntity<List<ContratResponse>> echeances(
            @RequestParam(defaultValue = "30") int jours,
            Authentication authentication) {
        currentUser(authentication);
        return ResponseEntity.ok(contratService.findEcheances(jours));
    }

    private Utilisateur currentUser(Authentication authentication) {
        return utilisateurRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Utilisateur authentifié introuvable"));
    }
}
