package tovo.gestion.rh.conge.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tovo.gestion.rh.conge.dto.*;
import tovo.gestion.rh.conge.service.DemandeCongeService;
import tovo.gestion.rh.temps.service.TempsPlanningService;
import java.time.LocalDate;
import java.util.Set;

@RestController @RequestMapping("/api") @RequiredArgsConstructor
public class DemandeCongeController {
    private final DemandeCongeService service;
    private final TempsPlanningService planningAccess;

    @PostMapping("/demandes-conge") @PreAuthorize("hasAnyRole('EMPLOYE','MANAGER','RH','ADMIN')")
    public ResponseEntity<?> creer(@Valid @RequestBody DemandeCongeRequest r,Authentication auth){return ResponseEntity.ok(service.creer(auth.getName(),r));}
    @PostMapping("/demandes-conge/{id}/soumettre") @PreAuthorize("hasAnyRole('EMPLOYE','MANAGER','RH','ADMIN')")
    public ResponseEntity<?> soumettre(@PathVariable long id,Authentication auth){return ResponseEntity.ok(service.soumettre(id,auth.getName()));}
    @PostMapping("/demandes-conge/{id}/valider-manager") @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> validerManager(@PathVariable long id,Authentication auth){return ResponseEntity.ok(service.validerManager(id,auth.getName()));}
    @PostMapping("/demandes-conge/{id}/valider-rh") @PreAuthorize("hasAnyRole('RH','ADMIN')")
    public ResponseEntity<?> validerRh(@PathVariable long id,Authentication auth){return ResponseEntity.ok(service.validerRh(id,auth.getName()));}
    @PostMapping("/demandes-conge/{id}/refuser") @PreAuthorize("hasAnyRole('MANAGER','RH','ADMIN')")
    public ResponseEntity<?> refuser(@PathVariable long id,@Valid @RequestBody MotifDemandeRequest r,Authentication auth){return ResponseEntity.ok(service.refuser(id,auth.getName(),r.motif()));}
    @PostMapping("/demandes-conge/{id}/annuler") @PreAuthorize("hasAnyRole('EMPLOYE','MANAGER','RH','ADMIN')")
    public ResponseEntity<?> annuler(@PathVariable long id,Authentication auth){return ResponseEntity.ok(service.annuler(id,auth.getName()));}
    @GetMapping("/demandes-conge") @PreAuthorize("hasAnyRole('EMPLOYE','MANAGER','RH','ADMIN')")
    public ResponseEntity<?> lister(@RequestParam(required=false) String statut,
          @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate du,
          @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate au,
          @RequestParam(required=false) Integer departement,Authentication auth){
        if(statut!=null&&!Set.of("BROUILLON","SOUMISE","VALIDEE_MANAGER","VALIDEE_RH","REFUSEE","ANNULEE").contains(statut))throw new IllegalArgumentException("Statut invalide");
        if(du!=null&&au!=null&&au.isBefore(du))throw new IllegalArgumentException("La période est invalide");
        String role=auth.getAuthorities().stream().map(a->a.getAuthority()).filter(a->a.startsWith("ROLE_")).map(a->a.substring(5)).filter(Set.of("RH","ADMIN","MANAGER","EMPLOYE")::contains).findFirst().orElse("EMPLOYE");
        return ResponseEntity.ok(service.lister(auth.getName(),role,statut,du,au,departement));
    }
    @GetMapping("/equipes/{id}/calendrier-conges") @PreAuthorize("hasAnyRole('MANAGER','RH','ADMIN')")
    public ResponseEntity<?> calendrier(@PathVariable int id,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate du,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate au,Authentication auth){boolean privileged=has(auth,"RH")||has(auth,"ADMIN");if(!privileged)planningAccess.verifierAccesEquipe(id,auth.getName());return ResponseEntity.ok(service.calendrierEquipe(id,du,au,privileged?null:auth.getName()));}
    @GetMapping("/delegations-validation") @PreAuthorize("hasRole('MANAGER')") public ResponseEntity<?> delegations(Authentication auth){return ResponseEntity.ok(service.delegations(auth.getName()));}
    @PostMapping("/delegations-validation") @PreAuthorize("hasRole('MANAGER')") public ResponseEntity<?> creerDelegation(@Valid @RequestBody DelegationValidationRequest r,Authentication auth){return ResponseEntity.ok(service.ajouterDelegation(auth.getName(),r.delegueId(),r.dateDebut(),r.dateFin()));}
    @DeleteMapping("/delegations-validation/{id}") @PreAuthorize("hasRole('MANAGER')") public ResponseEntity<Void> supprimerDelegation(@PathVariable long id,Authentication auth){service.supprimerDelegation(auth.getName(),id);return ResponseEntity.noContent().build();}
    @GetMapping("/politique-demandes-conge") @PreAuthorize("hasAnyRole('RH','ADMIN')") public ResponseEntity<?> politique(){return ResponseEntity.ok(service.politique());}
    @PutMapping("/politique-demandes-conge") @PreAuthorize("hasAnyRole('RH','ADMIN')") public ResponseEntity<?> modifierPolitique(@Valid @RequestBody PolitiqueDemandeCongeRequest r){return ResponseEntity.ok(service.modifierPolitique(r.preavisMinJours(),r.presenceMinPct(),r.rappelValidationJours(),r.seuilSoldeFaibleJours()));}
    @GetMapping("/periodes-bloquees-conge") @PreAuthorize("hasAnyRole('RH','ADMIN')") public ResponseEntity<?> periodes(){return ResponseEntity.ok(service.periodesBloquees());}
    @PostMapping("/periodes-bloquees-conge") @PreAuthorize("hasAnyRole('RH','ADMIN')") public ResponseEntity<?> ajouterPeriode(@Valid @RequestBody PeriodeBlocageCongeRequest r){return ResponseEntity.ok(service.ajouterPeriodeBloquee(r.dateDebut(),r.dateFin(),r.libelle()));}
    @DeleteMapping("/periodes-bloquees-conge/{id}") @PreAuthorize("hasAnyRole('RH','ADMIN')") public ResponseEntity<Void> supprimerPeriode(@PathVariable long id){service.supprimerPeriodeBloquee(id);return ResponseEntity.noContent().build();}
    private boolean has(Authentication auth,String role){return auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_"+role));}
}
