package tovo.gestion.rh.temps.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tovo.gestion.rh.temps.service.AnomalieService;
import tovo.gestion.rh.temps.service.HeureSupService;
import java.time.LocalDate;

@RestController @RequestMapping("/api/anomalies") @RequiredArgsConstructor
public class AnomalieController {
    private final AnomalieService service;
    private final HeureSupService utilisateur;

    @GetMapping @PreAuthorize("hasAnyRole('EMPLOYE','MANAGER','RH','ADMIN')")
    public ResponseEntity<?> rechercher(@RequestParam(required=false) String statut,@RequestParam(required=false) String type,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate du,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate au,
            @RequestParam(required=false) Integer departementId,Authentication auth){return ResponseEntity.ok(service.rechercher(auth.getName(),role(auth),statut,type,du,au,departementId));}

    @PostMapping("/{id}/justifier") @PreAuthorize("hasAnyRole('EMPLOYE','MANAGER','RH','ADMIN')")
    public ResponseEntity<?> justifier(@PathVariable long id,@Valid @RequestBody TraitementRequest r,Authentication auth){return traiter(id,r,auth,"justifier");}
    @PostMapping("/{id}/corriger") @PreAuthorize("hasAnyRole('MANAGER','RH','ADMIN')")
    public ResponseEntity<?> corriger(@PathVariable long id,@Valid @RequestBody TraitementRequest r,Authentication auth){return traiter(id,r,auth,"corriger");}
    @PostMapping("/{id}/ignorer") @PreAuthorize("hasAnyRole('MANAGER','RH','ADMIN')")
    public ResponseEntity<?> ignorer(@PathVariable long id,@Valid @RequestBody TraitementRequest r,Authentication auth){return traiter(id,r,auth,"ignorer");}

    private ResponseEntity<?> traiter(long id,TraitementRequest r,Authentication a,String action){return ResponseEntity.ok(service.traiter(id,a.getName(),utilisateur.utilisateurId(a.getName()),role(a),action,r.commentaire()));}
    private static String role(Authentication a){return a.getAuthorities().stream().map(x->x.getAuthority().replaceFirst("^ROLE_","")).findFirst().orElse("");}
    public record TraitementRequest(@NotBlank @Size(max=2000) String commentaire){}
}
