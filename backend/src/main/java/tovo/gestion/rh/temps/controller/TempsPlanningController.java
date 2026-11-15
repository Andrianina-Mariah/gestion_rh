package tovo.gestion.rh.temps.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tovo.gestion.rh.temps.dto.*;
import tovo.gestion.rh.temps.service.TempsPlanningService;

import java.time.LocalDate;
import java.util.List;

@RestController @RequestMapping("/api") @RequiredArgsConstructor
public class TempsPlanningController {
    private final TempsPlanningService service;

    @GetMapping("/regimes-travail") @PreAuthorize("hasAnyRole('RH','ADMIN','MANAGER')")
    public ResponseEntity<?> regimes(){return ResponseEntity.ok(service.regimes());}
    @PostMapping("/regimes-travail") @PreAuthorize("hasAnyRole('RH','ADMIN')")
    public ResponseEntity<?> creerRegime(@Valid @RequestBody RegimeRequest r){return ResponseEntity.ok(service.creerRegime(r));}
    @PutMapping("/regimes-travail/{id}") @PreAuthorize("hasAnyRole('RH','ADMIN')")
    public ResponseEntity<?> modifierRegime(@PathVariable long id,@Valid @RequestBody RegimeRequest r){return ResponseEntity.ok(service.modifierRegime(id,r));}
    @DeleteMapping("/regimes-travail/{id}") @PreAuthorize("hasAnyRole('RH','ADMIN')")
    public ResponseEntity<Void> supprimerRegime(@PathVariable long id){service.supprimerRegime(id);return ResponseEntity.noContent().build();}
    @GetMapping("/regimes-travail/{id}/horaires") @PreAuthorize("hasAnyRole('RH','ADMIN','MANAGER')")
    public ResponseEntity<?> horaires(@PathVariable long id){return ResponseEntity.ok(service.horaires(id));}
    @PutMapping("/regimes-travail/{id}/horaires") @PreAuthorize("hasAnyRole('RH','ADMIN')")
    public ResponseEntity<?> horaires(@PathVariable long id,@Valid @RequestBody List<@Valid HoraireTypeRequest> horaires){return ResponseEntity.ok(service.remplacerHoraires(id,horaires));}
    @PostMapping("/employe-regimes") @PreAuthorize("hasAnyRole('RH','ADMIN')")
    public ResponseEntity<Void> assigner(@Valid @RequestBody AssignationRegimeRequest r){service.assigner(r);return ResponseEntity.noContent().build();}
    @GetMapping("/jours-feries") @PreAuthorize("hasAnyRole('RH','ADMIN','MANAGER','EMPLOYE')")
    public ResponseEntity<?> joursFeries(@RequestParam(required=false) Integer annee){return ResponseEntity.ok(service.joursFeries(annee));}
    @PostMapping("/jours-feries") @PreAuthorize("hasAnyRole('RH','ADMIN')")
    public ResponseEntity<?> creerJourFerie(@Valid @RequestBody JourFerieRequest r){return ResponseEntity.ok(service.creerJourFerie(r));}
    @PostMapping("/plannings/generer") @PreAuthorize("hasAnyRole('RH','ADMIN')")
    public ResponseEntity<?> generer(@Valid @RequestBody GenererPlanningRequest r){return ResponseEntity.ok(java.util.Map.of("lignesCreees",service.generer(r)));}
    @PostMapping("/plannings/modifier") @PreAuthorize("hasAnyRole('RH','ADMIN','MANAGER')")
    public ResponseEntity<?> modifier(@Valid @RequestBody ModifierPlanningRequest r,Authentication auth){boolean manager=auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_MANAGER"));boolean privileged=auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_RH")||a.getAuthority().equals("ROLE_ADMIN"));if(!privileged)service.verifierAccesEmploye(r.employeId(),auth.getName(),manager);return ResponseEntity.ok(service.modifierPlanning(r));}
    @GetMapping("/employes/{id}/planning") @PreAuthorize("hasAnyRole('RH','ADMIN','MANAGER','EMPLOYE')")
    public ResponseEntity<?> planning(@PathVariable long id,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate du,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate au,Authentication auth){if(au.isBefore(du))throw new IllegalArgumentException("La date de fin précède la date de début");boolean manager=auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_MANAGER"));boolean privileged=auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_RH")||a.getAuthority().equals("ROLE_ADMIN"));if(!privileged)service.verifierAccesEmploye(id,auth.getName(),manager);return ResponseEntity.ok(service.planningEmploye(id,du,au));}
    @GetMapping("/departements/{id}/planning") @PreAuthorize("hasAnyRole('RH','ADMIN','MANAGER')")
    public ResponseEntity<?> equipe(@PathVariable int id,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate du,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate au,Authentication auth){if(au.isBefore(du))throw new IllegalArgumentException("La date de fin précède la date de début");boolean privileged=auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_RH")||a.getAuthority().equals("ROLE_ADMIN"));if(!privileged)service.verifierAccesEquipe(id,auth.getName());return ResponseEntity.ok(service.planningEquipe(id,du,au,privileged?null:auth.getName()));}
}
