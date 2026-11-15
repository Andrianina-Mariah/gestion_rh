package tovo.gestion.rh.candidature.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tovo.gestion.rh.candidature.dto.CandidatureRequest;
import tovo.gestion.rh.candidature.dto.CandidatureResponse;
import tovo.gestion.rh.candidature.dto.DecisionDossierRequest;
import tovo.gestion.rh.candidature.dto.DecisionFinaleRequest;
import tovo.gestion.rh.candidature.dto.PlanifierEvaluationRequest;
import tovo.gestion.rh.candidature.dto.ResultatEvaluationRequest;
import tovo.gestion.rh.candidature.dto.EvaluationResponse;
import tovo.gestion.rh.candidature.entity.StatutCandidature;
import tovo.gestion.rh.candidature.service.CandidatureService;
import tovo.gestion.rh.candidature.service.SelectionDossierService;
import tovo.gestion.rh.candidature.service.EvaluationService;
import tovo.gestion.rh.utilisateur.entity.Utilisateur;
import tovo.gestion.rh.utilisateur.repository.UtilisateurRepository;
import org.springframework.security.core.Authentication;

@RestController
@RequiredArgsConstructor
public class CandidatureController {
    private final CandidatureService candidatureService;
    private final SelectionDossierService selectionService;
    private final EvaluationService evaluationService;
    private final UtilisateurRepository utilisateurRepository;

    @PostMapping(value = "/api/public/offres/{offreId}/candidatures",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CandidatureResponse> postuler(
            @PathVariable Long offreId,
            @Valid @RequestPart("candidature") CandidatureRequest request,
            @RequestPart(value = "cv", required = false) MultipartFile cv,
            @RequestPart(value = "lettreMotivation", required = false) MultipartFile lettreMotivation) {
        return ResponseEntity.ok(candidatureService.postuler(offreId, request, cv, lettreMotivation));
    }

    @GetMapping("/api/offres/{offreId}/candidatures")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'MANAGER')")
    public ResponseEntity<Page<CandidatureResponse>> findByOffre(
            @PathVariable Long offreId,
            @RequestParam(required = false) StatutCandidature statut,
            @PageableDefault(size = 20, sort = "dateCandidature") Pageable pageable,
            Authentication authentication) {
        return ResponseEntity.ok(candidatureService.findByOffre(
                offreId, statut, pageable, currentUser(authentication)));
    }

    @GetMapping("/api/candidatures/{id}")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'MANAGER')")
    public ResponseEntity<CandidatureResponse> findById(
            @PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(candidatureService.findById(id, currentUser(authentication)));
    }

    @PostMapping("/api/candidatures/{id}/decision-dossier")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<CandidatureResponse> decisionDossier(
            @PathVariable Long id,
            @Valid @RequestBody DecisionDossierRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(selectionService.decider(
                id, request, currentUser(authentication)));
    }

    @PostMapping("/api/candidatures/{id}/promouvoir")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<CandidatureResponse> promouvoir(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(selectionService.promouvoir(
                id, currentUser(authentication)));
    }

    @GetMapping("/api/offres/{offreId}/candidatures/liste-attente")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'MANAGER')")
    public ResponseEntity<java.util.List<CandidatureResponse>> listeAttente(
            @PathVariable Long offreId, Authentication authentication) {
        return ResponseEntity.ok(selectionService.listeAttente(offreId, currentUser(authentication)));
    }

    @PostMapping("/api/candidatures/{id}/evaluations")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'MANAGER')")
    public ResponseEntity<EvaluationResponse> planifierEvaluation(
            @PathVariable Long id, @Valid @RequestBody PlanifierEvaluationRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(evaluationService.planifier(id, request, currentUser(authentication)));
    }

    @PutMapping("/api/evaluations/{id}/resultat")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'MANAGER')")
    public ResponseEntity<EvaluationResponse> saisirResultat(
            @PathVariable Long id, @Valid @RequestBody ResultatEvaluationRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(evaluationService.saisirResultat(id, request, currentUser(authentication)));
    }

    @GetMapping("/api/candidatures/{id}/evaluations")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'MANAGER')")
    public ResponseEntity<java.util.List<EvaluationResponse>> evaluations(
            @PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(evaluationService.findByCandidature(id, currentUser(authentication)));
    }

    @PostMapping("/api/candidatures/{id}/decision-finale")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    public ResponseEntity<CandidatureResponse> decisionFinale(
            @PathVariable Long id, @Valid @RequestBody DecisionFinaleRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(evaluationService.decisionFinale(id, request, currentUser(authentication)));
    }

    @GetMapping("/api/offres/{offreId}/candidats-retenus")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'MANAGER')")
    public ResponseEntity<java.util.List<CandidatureResponse>> retenus(
            @PathVariable Long offreId, Authentication authentication) {
        return ResponseEntity.ok(evaluationService.retenus(offreId, currentUser(authentication)));
    }

    @GetMapping("/api/candidatures/files/{filename:.+}")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    public ResponseEntity<Resource> file(@PathVariable String filename) {
        Resource resource = candidatureService.loadFile(filename);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentType(org.springframework.http.MediaTypeFactory.getMediaType(resource)
                        .orElse(MediaType.APPLICATION_OCTET_STREAM))
                .body(resource);
    }

    private Utilisateur currentUser(Authentication authentication) {
        return utilisateurRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Utilisateur authentifié introuvable"));
    }
}
