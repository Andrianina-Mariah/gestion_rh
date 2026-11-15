package tovo.gestion.rh.workflow.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tovo.gestion.rh.demanderecrutement.dto.DemandeResponse;
import tovo.gestion.rh.demanderecrutement.dto.DemandeListItem;
import tovo.gestion.rh.demanderecrutement.mapper.DemandeMapper;
import tovo.gestion.rh.utilisateur.repository.UtilisateurRepository;
import tovo.gestion.rh.workflow.DecisionValidation;
import tovo.gestion.rh.workflow.dto.DecisionValidationRequest;
import tovo.gestion.rh.workflow.dto.ValidationHistoryItem;
import tovo.gestion.rh.workflow.service.WorkflowService;

import java.util.List;

@RestController
@RequestMapping("/api/demandes")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;
    private final UtilisateurRepository utilisateurRepository;
    private final DemandeMapper demandeMapper;

    @Operation(summary = "Décision RH sur une demande")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Décision appliquée"),
            @ApiResponse(responseCode = "409", description = "Transition ou validation interdite"),
            @ApiResponse(responseCode = "403", description = "Rôle insuffisant"),
            @ApiResponse(responseCode = "404", description = "Demande introuvable")
    })
    @PostMapping("/{id}/valider-rh")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<DemandeResponse> validerRh(
            @PathVariable Integer id,
            @Valid @RequestBody DecisionValidationRequest request,
            Authentication authentication
    ) {
        return decision(id, request, authentication);
    }

    @Operation(summary = "Décision Finance sur une demande")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Décision appliquée"),
            @ApiResponse(responseCode = "409", description = "Transition ou budget invalide"),
            @ApiResponse(responseCode = "403", description = "Rôle insuffisant"),
            @ApiResponse(responseCode = "404", description = "Demande introuvable")
    })
    @PostMapping("/{id}/valider-finance")
    @PreAuthorize("hasRole('FINANCE')")
    public ResponseEntity<DemandeResponse> validerFinance(
            @PathVariable Integer id,
            @Valid @RequestBody DecisionValidationRequest request,
            Authentication authentication
    ) {
        return decision(id, request, authentication);
    }

    @Operation(summary = "Décision Direction sur une demande")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Décision appliquée"),
            @ApiResponse(responseCode = "409", description = "Transition interdite"),
            @ApiResponse(responseCode = "403", description = "Rôle insuffisant"),
            @ApiResponse(responseCode = "404", description = "Demande introuvable")
    })
    @PostMapping("/{id}/valider-direction")
    @PreAuthorize("hasRole('DIRECTION')")
    public ResponseEntity<DemandeResponse> validerDirection(
            @PathVariable Integer id,
            @Valid @RequestBody DecisionValidationRequest request,
            Authentication authentication
    ) {
        return decision(id, request, authentication);
    }

    @Operation(summary = "Resoumettre une demande à corriger")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Demande resoumise"),
            @ApiResponse(responseCode = "409", description = "Demande non resoumissible"),
            @ApiResponse(responseCode = "403", description = "Rôle insuffisant")
    })
    @PostMapping("/{id}/resoumettre")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<DemandeResponse> resoumettre(
            @PathVariable Integer id,
            Authentication authentication
    ) {
        var manager = currentUser(authentication);
        return ResponseEntity.ok(demandeMapper.toResponse(
                workflowService.resoumettre(id, manager)
        ));
    }

    @Operation(summary = "Ouvrir le recrutement d'une demande validée")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recrutement ouvert"),
            @ApiResponse(responseCode = "409", description = "Demande non validée"),
            @ApiResponse(responseCode = "403", description = "Rôle insuffisant")
    })
    @PostMapping("/{id}/ouvrir-recrutement")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<DemandeResponse> ouvrirRecrutement(
            @PathVariable Integer id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(demandeMapper.toResponse(
                workflowService.ouvrirRecrutement(id, currentUser(authentication))
        ));
    }

    @Operation(summary = "Consulter l'historique des validations")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historique chronologique"),
            @ApiResponse(responseCode = "404", description = "Demande introuvable")
    })
    @GetMapping("/{id}/historique")
    public ResponseEntity<List<ValidationHistoryItem>> historique(
            @PathVariable Integer id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(workflowService.historique(id, currentUser(authentication)));
    }

    @Operation(summary = "Lister les demandes à traiter par l'utilisateur connecté")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Demandes correspondant à l'étape"),
            @ApiResponse(responseCode = "403", description = "Rôle non habilité")
    })
    @GetMapping("/a-traiter")
    @PreAuthorize("hasAnyRole('RH', 'FINANCE', 'DIRECTION')")
    public ResponseEntity<List<DemandeListItem>> aTraiter(Authentication authentication) {
        return ResponseEntity.ok(workflowService.demandesATraiter(currentUser(authentication))
                .stream()
                .map(demandeMapper::toListItem)
                .toList());
    }

    private ResponseEntity<DemandeResponse> decision(
            Integer id,
            DecisionValidationRequest request,
            Authentication authentication
    ) {
        var utilisateur = currentUser(authentication);
        var updated = workflowService.appliquerDecision(
                id,
                utilisateur,
                request.getDecision(),
                request.getCommentaire(),
                request.getAnalyseBudget(),
                request.getBudgetValide()
        );
        return ResponseEntity.ok(demandeMapper.toResponse(updated));
    }

    private tovo.gestion.rh.utilisateur.entity.Utilisateur currentUser(Authentication authentication) {
        return utilisateurRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Utilisateur authentifié introuvable"));
    }
}
