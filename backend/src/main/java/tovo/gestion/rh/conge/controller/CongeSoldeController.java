package tovo.gestion.rh.conge.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tovo.gestion.rh.conge.dto.*;
import tovo.gestion.rh.conge.service.CongeSoldeService;
import tovo.gestion.rh.temps.service.TempsPlanningService;
import tovo.gestion.rh.utilisateur.repository.UtilisateurRepository;
import java.time.Year;

@RestController @RequestMapping("/api") @RequiredArgsConstructor
public class CongeSoldeController {
    private final CongeSoldeService service;
    private final TempsPlanningService access;
    private final UtilisateurRepository utilisateurs;

    @GetMapping("/conges/types") @PreAuthorize("hasAnyRole('RH','ADMIN','MANAGER','EMPLOYE')") public Object types(){return service.types();}
    @PostMapping("/conges/types") @PreAuthorize("hasAnyRole('RH','ADMIN')") public Object creerType(@Valid @RequestBody TypeCongeRequest r){return service.creerType(r);}
    @PutMapping("/conges/types/{id}") @PreAuthorize("hasAnyRole('RH','ADMIN')") public Object modifierType(@PathVariable long id,@Valid @RequestBody TypeCongeRequest r){return service.modifierType(id,r);}
    @DeleteMapping("/conges/types/{id}") @PreAuthorize("hasAnyRole('RH','ADMIN')") public ResponseEntity<Void> supprimerType(@PathVariable long id){service.supprimerType(id);return ResponseEntity.noContent().build();}
    @GetMapping("/conges/types/{id}/droits") @PreAuthorize("hasAnyRole('RH','ADMIN','MANAGER','EMPLOYE')") public Object droits(@PathVariable long id){return service.droits(id);}
    @PostMapping("/conges/droits") @PreAuthorize("hasAnyRole('RH','ADMIN')") public Object creerDroit(@Valid @RequestBody DroitCongeRequest r){return service.creerDroit(r);}
    @PutMapping("/conges/droits/{id}") @PreAuthorize("hasAnyRole('RH','ADMIN')") public Object modifierDroit(@PathVariable long id,@Valid @RequestBody DroitCongeRequest r){return service.modifierDroit(id,r);}
    @DeleteMapping("/conges/droits/{id}") @PreAuthorize("hasAnyRole('RH','ADMIN')") public ResponseEntity<Void> supprimerDroit(@PathVariable long id){service.supprimerDroit(id);return ResponseEntity.noContent().build();}

    @GetMapping("/employes/{id}/soldes") @PreAuthorize("hasAnyRole('RH','ADMIN','MANAGER','EMPLOYE')") public Object soldes(@PathVariable long id,@RequestParam(required=false) Integer annee,Authentication auth){authorize(id,auth);return service.soldes(id,annee);}
    @GetMapping("/moi/soldes") @PreAuthorize("hasAnyRole('EMPLOYE','MANAGER','RH','ADMIN')") public Object mesSoldes(@RequestParam(required=false) Integer annee,Authentication auth){return service.soldes(service.employeConnecte(auth.getName()),annee);}
    @PostMapping("/employes/{id}/soldes/ajustements") @PreAuthorize("hasAnyRole('RH','ADMIN')") public Object ajuster(@PathVariable long id,@Valid @RequestBody AjustementSoldeRequest r,Authentication auth){var user=utilisateurs.findByEmail(auth.getName()).orElseThrow();return service.ajuster(id,r,user.getId(),user.getRole().getCode());}
    @GetMapping("/rapports/soldes-conges") @PreAuthorize("hasAnyRole('RH','ADMIN')") public Object rapportSoldes(@RequestParam(required=false) Integer annee){return service.rapportSoldes(annee==null?Year.now().getValue():annee);}
    private void authorize(long id,Authentication auth){boolean privileged=auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_RH")||a.getAuthority().equals("ROLE_ADMIN"));boolean manager=auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_MANAGER"));if(!privileged)access.verifierAccesEmploye(id,auth.getName(),manager);}
}
