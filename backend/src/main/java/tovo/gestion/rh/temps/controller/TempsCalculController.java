package tovo.gestion.rh.temps.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tovo.gestion.rh.temps.dto.RegleTempsRequest;
import tovo.gestion.rh.temps.service.TempsCalculService;
import tovo.gestion.rh.temps.service.TempsPlanningService;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

@RestController @RequestMapping("/api") @RequiredArgsConstructor
public class TempsCalculController {
    private final TempsCalculService calcul;
    private final TempsPlanningService access;

    @GetMapping("/employes/{id}/temps/recap") @PreAuthorize("hasAnyRole('EMPLOYE','MANAGER','RH','ADMIN')")
    public ResponseEntity<?> recap(@PathVariable long id,@RequestParam @DateTimeFormat(pattern="yyyy-MM") YearMonth mois,Authentication auth){
        boolean manager=hasRole(auth,"MANAGER"),privileged=hasRole(auth,"RH")||hasRole(auth,"ADMIN");if(!privileged)access.verifierAccesEmploye(id,auth.getName(),manager);
        return ResponseEntity.ok(calcul.recap(id,mois));
    }

    @PostMapping("/employes/{id}/temps/recalculer") @PreAuthorize("hasAnyRole('MANAGER','RH','ADMIN')")
    public ResponseEntity<?> recalculer(@PathVariable long id,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date,Authentication auth){
        boolean manager=hasRole(auth,"MANAGER"),privileged=hasRole(auth,"RH")||hasRole(auth,"ADMIN");if(!privileged)access.verifierAccesEmploye(id,auth.getName(),manager);
        return ResponseEntity.ok(calcul.recalculer(id,date,false));
    }

    @GetMapping("/temps/regles") @PreAuthorize("hasAnyRole('RH','ADMIN')")
    public ResponseEntity<?> regles(){return ResponseEntity.ok(calcul.regles());}
    @PutMapping("/temps/regles") @PreAuthorize("hasAnyRole('RH','ADMIN')")
    public ResponseEntity<Map<String,Object>> modifierRegles(@Valid @RequestBody RegleTempsRequest request){return ResponseEntity.ok(calcul.modifierRegles(request));}

    private static boolean hasRole(Authentication auth,String role){return auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_"+role));}
}
