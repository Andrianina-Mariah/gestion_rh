package tovo.gestion.rh.onboarding.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import tovo.gestion.rh.onboarding.dto.*;
import tovo.gestion.rh.onboarding.entity.InscriptionPaie;
import tovo.gestion.rh.onboarding.service.OnboardingService;
import tovo.gestion.rh.utilisateur.entity.Utilisateur;
import tovo.gestion.rh.utilisateur.repository.UtilisateurRepository;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class OnboardingController {
    private final OnboardingService onboardingService;
    private final UtilisateurRepository utilisateurRepository;

    @GetMapping("/api/onboardings")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'IT', 'MANAGER', 'SERVICES_GENERAUX', 'EMPLOYE')")
    public ResponseEntity<List<OnboardingResponse>> findAll(
            @RequestParam(required = false) String statut,
            Authentication authentication) {
        return ResponseEntity.ok(onboardingService.findAll(statut, currentUser(authentication)));
    }

    @GetMapping("/api/onboardings/{id}")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'IT', 'MANAGER', 'SERVICES_GENERAUX', 'EMPLOYE')")
    public ResponseEntity<OnboardingResponse> findById(
            @PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(onboardingService.findById(id, currentUser(authentication)));
    }

    @GetMapping("/api/employes/{id}/onboarding")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'IT', 'MANAGER', 'SERVICES_GENERAUX')")
    public ResponseEntity<OnboardingResponse> findByEmploye(
            @PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(onboardingService.findByEmploye(id, currentUser(authentication)));
    }

    @PostMapping("/api/onboarding-taches/{id}/demarrer")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'IT', 'MANAGER', 'SERVICES_GENERAUX')")
    public ResponseEntity<OnboardingTacheResponse> demarrer(
            @PathVariable Long id, @Valid @RequestBody CommentaireTacheRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(onboardingService.demarrer(id, request, currentUser(authentication)));
    }

    @PostMapping("/api/onboarding-taches/{id}/terminer")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'IT', 'MANAGER', 'SERVICES_GENERAUX')")
    public ResponseEntity<OnboardingTacheResponse> terminer(
            @PathVariable Long id, @Valid @RequestBody CommentaireTacheRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(onboardingService.terminer(id, request, currentUser(authentication)));
    }

    @PostMapping("/api/onboarding-taches/{id}/non-applicable")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'IT', 'MANAGER', 'SERVICES_GENERAUX')")
    public ResponseEntity<OnboardingTacheResponse> nonApplicable(
            @PathVariable Long id, @Valid @RequestBody CommentaireTacheRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(onboardingService.nonApplicable(id, request, currentUser(authentication)));
    }

    @PostMapping("/api/onboardings/{id}/taches")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'IT', 'MANAGER', 'SERVICES_GENERAUX')")
    public ResponseEntity<OnboardingTacheResponse> ajouterTache(
            @PathVariable Long id, @Valid @RequestBody CreerOnboardingTacheRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(onboardingService.ajouterTache(id, request, currentUser(authentication)));
    }

    @PatchMapping("/api/onboarding-taches/{id}/responsable")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'IT', 'MANAGER', 'SERVICES_GENERAUX')")
    public ResponseEntity<OnboardingTacheResponse> reassigner(
            @PathVariable Long id, @Valid @RequestBody ReassignerTacheRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(onboardingService.reassigner(id, request, currentUser(authentication)));
    }

    @PostMapping("/api/onboardings/{id}/cloturer")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    public ResponseEntity<OnboardingResponse> cloturer(
            @PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(onboardingService.cloturer(id, currentUser(authentication)));
    }

    @PostMapping("/api/onboarding-taches/{id}/badge")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'SERVICES_GENERAUX')")
    public ResponseEntity<OnboardingTacheResponse> badge(@PathVariable Long id,
            @Valid @RequestBody BadgeRequest request, Authentication authentication) {
        return ResponseEntity.ok(onboardingService.badge(id, request, currentUser(authentication)));
    }

    @PostMapping("/api/onboarding-taches/{id}/materiels")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'IT')")
    public ResponseEntity<MaterielResponse> materiel(@PathVariable Long id,
            @Valid @RequestBody MaterielRequest request, Authentication authentication) {
        return ResponseEntity.ok(onboardingService.materiel(id, request, currentUser(authentication)));
    }

    @PostMapping("/api/onboarding-taches/{id}/formation")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'MANAGER')")
    public ResponseEntity<FormationAccueilResponse> formation(@PathVariable Long id,
            @Valid @RequestBody FormationAccueilRequest request, Authentication authentication) {
        return ResponseEntity.ok(onboardingService.formation(id, request, currentUser(authentication)));
    }

    @PostMapping("/api/onboardings/{id}/paie")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    public ResponseEntity<InscriptionPaie> paie(@PathVariable Long id,
            @Valid @RequestBody InscriptionPaieRequest request, Authentication authentication) {
        return ResponseEntity.ok(onboardingService.paie(id, request, currentUser(authentication)));
    }

    @PostMapping(value = "/api/employes/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'IT', 'MANAGER', 'EMPLOYE')")
    public ResponseEntity<DocumentResponse> document(@PathVariable Long id,
            @RequestParam String type, @RequestParam(required = false) Long tacheId,
            @RequestParam(required = false) java.time.LocalDate dateEmission,
            @RequestParam(required = false) java.time.LocalDate dateExpiration,
            @RequestPart MultipartFile file, Authentication authentication) {
        return ResponseEntity.ok(onboardingService.document(id, tacheId, type, file, dateEmission,
                dateExpiration, currentUser(authentication)));
    }

    @GetMapping("/api/onboarding-documents/{id}")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'IT', 'MANAGER', 'EMPLOYE')")
    public ResponseEntity<ByteArrayResource> document(@PathVariable Long id, Authentication authentication) {
        var document = onboardingService.getDocument(id, currentUser(authentication));
        try {
            byte[] content = java.nio.file.Files.readAllBytes(java.nio.file.Path.of(".",
                    document.getUrl().replaceFirst("^/", "")));
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + document.getNomFichier() + "\"")
                    .body(new ByteArrayResource(content));
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Impossible de lire le document", e);
        }
    }

    @PostMapping("/api/documents/{id}/valider")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    public ResponseEntity<DocumentResponse> validerDocument(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(onboardingService.validateDocument(id, currentUser(authentication)));
    }

    @PostMapping("/api/documents/{id}/rejeter")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    public ResponseEntity<DocumentResponse> rejeterDocument(@PathVariable Long id,
            @RequestBody DocumentDecisionRequest request, Authentication authentication) {
        return ResponseEntity.ok(onboardingService.rejectDocument(id, request.motif(), currentUser(authentication)));
    }

    @GetMapping("/api/employes/{id}/documents/manquants")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'MANAGER', 'IT', 'EMPLOYE')")
    public ResponseEntity<List<DocumentManquantResponse>> documentsManquants(@PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(onboardingService.missingDocuments(id, currentUser(authentication)));
    }

    @GetMapping("/api/documents/manquants")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    public ResponseEntity<List<DocumentManquantResponse>> documentsManquantsGlobaux(Authentication authentication) {
        return ResponseEntity.ok(onboardingService.globalMissingDocuments(currentUser(authentication)));
    }

    @GetMapping("/api/documents/expirants")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    public ResponseEntity<List<DocumentAlerteResponse>> documentsExpirants(
            @RequestParam(defaultValue = "30") int jours, Authentication authentication) {
        return ResponseEntity.ok(onboardingService.expiringDocuments(jours, currentUser(authentication)));
    }

    private Utilisateur currentUser(Authentication authentication) {
        return utilisateurRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Utilisateur authentifié introuvable"));
    }
}
