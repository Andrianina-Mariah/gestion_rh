package tovo.gestion.rh.demanderecrutement.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tovo.gestion.rh.demanderecrutement.dto.*;
import tovo.gestion.rh.demanderecrutement.entity.StatutDemande;
import tovo.gestion.rh.demanderecrutement.service.DemandeRecrutementService;

@RestController
@RequestMapping("/api/demandes")
@RequiredArgsConstructor
public class DemandeRecrutementController {

    private final DemandeRecrutementService demandeService;

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<DemandeResponse> create(@Valid @RequestBody DemandeCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(demandeService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<DemandeResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody DemandeUpdateRequest request) {
        return ResponseEntity.ok(demandeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        demandeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/soumettre")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<DemandeResponse> soumettre(@PathVariable Integer id) {
        return ResponseEntity.ok(demandeService.soumettre(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DemandeResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(demandeService.findById(id));
    }

    @GetMapping
    public ResponseEntity<Page<DemandeListItem>> search(
            @RequestParam(required = false) StatutDemande statut,
            @RequestParam(required = false) Integer departementId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(demandeService.search(statut, departementId, pageable));
    }
}