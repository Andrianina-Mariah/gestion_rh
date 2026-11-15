package tovo.gestion.rh.embauche.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tovo.gestion.rh.embauche.dto.EmployeResponse;
import tovo.gestion.rh.embauche.dto.*;
import tovo.gestion.rh.embauche.service.EmployeService;
import tovo.gestion.rh.utilisateur.entity.Utilisateur;
import tovo.gestion.rh.utilisateur.repository.UtilisateurRepository;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class EmployeController {

    private final EmployeService employeService;
    private final UtilisateurRepository utilisateurRepository;

    @GetMapping("/employes")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'MANAGER', 'EMPLOYE')")
    public ResponseEntity<List<EmployeResponse>> findAll(
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) Integer departementId,
            @RequestParam(required = false) String poste,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "id") String sort,
            Authentication authentication) {
        Utilisateur utilisateur = currentUser(authentication);
        return ResponseEntity.ok(employeService.findAll(statut, departementId, poste, q, page, size, sort, utilisateur));
    }

    @GetMapping("/employes/{id}")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'MANAGER', 'EMPLOYE')")
    public ResponseEntity<EmployeResponse> findById(
            @PathVariable Long id,
            Authentication authentication) {
        Utilisateur utilisateur = currentUser(authentication);
        return ResponseEntity.ok(employeService.findById(id, utilisateur));
    }

    @GetMapping("/employes/{id}/dossier")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'MANAGER', 'EMPLOYE')")
    public ResponseEntity<DossierEmployeResponse> dossier(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(employeService.dossier(id, currentUser(authentication)));
    }

    @GetMapping("/employes/{id}/carriere")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'MANAGER', 'EMPLOYE')")
    public ResponseEntity<List<CarriereEventResponse>> carriere(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(employeService.carriere(id, currentUser(authentication)));
    }

    @PostMapping("/employes/{id}/affectations")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    public ResponseEntity<AffectationCareerResponse> affectation(@PathVariable Long id,
            @Valid @RequestBody AffectationRequest request, Authentication authentication) {
        return ResponseEntity.ok(employeService.changeAffectation(id, request, currentUser(authentication)));
    }

    @PostMapping("/employes/{id}/salaires")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    public ResponseEntity<SalaireResponse> salaire(@PathVariable Long id,
            @Valid @RequestBody SalaireRequest request, Authentication authentication) {
        return ResponseEntity.ok(employeService.changeSalaire(id, request, currentUser(authentication)));
    }

    @PostMapping("/employes/{id}/sortie")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    public ResponseEntity<EmployeResponse> sortie(@PathVariable Long id,
            @Valid @RequestBody SortieRequest request, Authentication authentication) {
        return ResponseEntity.ok(employeService.sortie(id, request, currentUser(authentication)));
    }

    @PutMapping("/employes/{id}/identite")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    public ResponseEntity<EmployeResponse> identite(@PathVariable Long id, @Valid @RequestBody IdentiteRequest request, Authentication authentication) {
        return ResponseEntity.ok(employeService.updateIdentite(id, request, currentUser(authentication)));
    }

    @PostMapping("/employes/{id}/adresses")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'EMPLOYE')")
    public ResponseEntity<AdresseResponse> adresse(@PathVariable Long id, @Valid @RequestBody AdresseRequest request, Authentication authentication) {
        return ResponseEntity.ok(employeService.addAddress(id, request, currentUser(authentication)));
    }

    @PostMapping("/employes/{id}/contacts")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'EMPLOYE')")
    public ResponseEntity<ContactResponse> contact(@PathVariable Long id, @Valid @RequestBody ContactRequest request, Authentication authentication) {
        return ResponseEntity.ok(employeService.addContact(id, request, currentUser(authentication)));
    }

    @PostMapping("/employes/{id}/contacts-urgence")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'EMPLOYE')")
    public ResponseEntity<ContactUrgenceResponse> contactUrgence(@PathVariable Long id,
            @Valid @RequestBody ContactUrgenceRequest request, Authentication authentication) {
        return ResponseEntity.ok(employeService.addEmergencyContact(id, request, currentUser(authentication)));
    }

    @PostMapping("/employes/{id}/ayants-droit")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN', 'EMPLOYE')")
    public ResponseEntity<AyantDroitResponse> ayantDroit(@PathVariable Long id,
            @Valid @RequestBody AyantDroitRequest request, Authentication authentication) {
        return ResponseEntity.ok(employeService.addDependent(id, request, currentUser(authentication)));
    }

    @RequestMapping(value = "/moi/dossier", method = {RequestMethod.POST, RequestMethod.PUT})
    @PreAuthorize("hasRole('EMPLOYE')")
    public ResponseEntity<DemandeModificationResponse> demandeModification(@Valid @RequestBody ModificationRequest request, Authentication authentication) {
        return ResponseEntity.ok(employeService.requestModification(request, currentUser(authentication)));
    }

    @GetMapping("/moi/dossier")
    @PreAuthorize("hasRole('EMPLOYE')")
    public ResponseEntity<DossierEmployeResponse> moi(Authentication authentication) {
        Utilisateur user = currentUser(authentication);
        return ResponseEntity.ok(employeService.dossierPersonnel(user));
    }

    @GetMapping("/demandes-modification")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    public ResponseEntity<List<DemandeModificationResponse>> demandes(Authentication authentication) {
        return ResponseEntity.ok(employeService.pendingModifications(currentUser(authentication)));
    }

    @PostMapping("/demandes-modification/{id}/approuver")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    public ResponseEntity<DemandeModificationResponse> approuver(@PathVariable Long id, @RequestBody(required = false) DecisionModificationRequest request, Authentication authentication) {
        return ResponseEntity.ok(employeService.decideModification(id, true, request, currentUser(authentication)));
    }

    @PostMapping("/demandes-modification/{id}/rejeter")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    public ResponseEntity<DemandeModificationResponse> rejeter(@PathVariable Long id, @RequestBody(required = false) DecisionModificationRequest request, Authentication authentication) {
        return ResponseEntity.ok(employeService.decideModification(id, false, request, currentUser(authentication)));
    }

    private Utilisateur currentUser(Authentication authentication) {
        return utilisateurRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Utilisateur authentifié introuvable"));
    }
}
