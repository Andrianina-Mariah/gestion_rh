package tovo.gestion.rh.temps.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tovo.gestion.rh.temps.dto.DemandeHeureSupRequest;
import tovo.gestion.rh.temps.service.HeureSupService;

import java.util.Map;
import java.time.LocalDate;

@RestController @RequestMapping("/api/heures-sup") @RequiredArgsConstructor
public class HeureSupController {
    private final HeureSupService service;
    @PostMapping @PreAuthorize("hasAnyRole('EMPLOYE','MANAGER','RH','ADMIN')")
    public ResponseEntity<Map<String,Object>> demander(@Valid @RequestBody DemandeHeureSupRequest r,Authentication auth){boolean manager=has(auth,"MANAGER"),privileged=has(auth,"RH")||has(auth,"ADMIN");return ResponseEntity.ok(service.demander(auth.getName(),privileged,manager,r));}
    @GetMapping @PreAuthorize("hasAnyRole('EMPLOYE','MANAGER','RH','ADMIN')")
    public ResponseEntity<?> rechercher(@RequestParam(required=false) Long employeId,@RequestParam(required=false) LocalDate date,@RequestParam(required=false) String statut,Authentication auth){return ResponseEntity.ok(service.rechercher(auth.getName(),has(auth,"MANAGER"),has(auth,"RH")||has(auth,"ADMIN"),employeId,date,statut));}
    @PostMapping("/{id}/valider") @PreAuthorize("hasAnyRole('MANAGER','RH','ADMIN')")
    public ResponseEntity<?> valider(@PathVariable long id,Authentication auth){return ResponseEntity.ok(service.decider(id,auth.getName(),service.utilisateurId(auth.getName()),has(auth,"MANAGER"),has(auth,"RH")||has(auth,"ADMIN"),true));}
    @PostMapping("/{id}/refuser") @PreAuthorize("hasAnyRole('MANAGER','RH','ADMIN')")
    public ResponseEntity<?> refuser(@PathVariable long id,Authentication auth){return ResponseEntity.ok(service.decider(id,auth.getName(),service.utilisateurId(auth.getName()),has(auth,"MANAGER"),has(auth,"RH")||has(auth,"ADMIN"),false));}
    private static boolean has(Authentication a,String r){return a.getAuthorities().stream().anyMatch(x->x.getAuthority().equals("ROLE_"+r));}
}
