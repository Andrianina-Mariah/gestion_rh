package tovo.gestion.rh.temps.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tovo.gestion.rh.temps.dto.CorrectionPointageRequest;
import tovo.gestion.rh.temps.dto.PointageRequest;
import tovo.gestion.rh.temps.service.PointageService;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;

@RestController @RequestMapping("/api") @RequiredArgsConstructor
public class PointageController {
    private final PointageService service;
    private final tovo.gestion.rh.temps.service.TempsPlanningService access;

    @PostMapping("/pointages") @PreAuthorize("hasAnyRole('EMPLOYE','MANAGER','RH','ADMIN')")
    public ResponseEntity<Map<String,Object>> pointer(@Valid @RequestBody PointageRequest request,Authentication auth){
        Map<String,Object> result=service.pointer(auth.getName(),request.sens());
        return ResponseEntity.status(Boolean.TRUE.equals(result.get("accepte"))?200:409).body(result);
    }

    @PostMapping(value="/pointages/import",consumes="multipart/form-data") @PreAuthorize("hasAnyRole('RH','ADMIN')")
    public ResponseEntity<?> importer(@RequestPart("fichier") MultipartFile fichier,Authentication auth) throws IOException {
        return ResponseEntity.ok(service.importer(fichier,auth.getName(),role(auth)));
    }

    @PostMapping("/pointages/corrections") @PreAuthorize("hasAnyRole('MANAGER','RH','ADMIN')")
    public ResponseEntity<?> corriger(@Valid @RequestBody CorrectionPointageRequest request,Authentication auth){
        boolean manager=hasRole(auth,"MANAGER"),rh=hasRole(auth,"RH")||hasRole(auth,"ADMIN");
        return ResponseEntity.ok(service.corriger(auth.getName(),rh,manager,role(auth),request.employeId(),request.pointageOrigineId(),request.dateHeure(),request.sens(),request.motif()));
    }

    @GetMapping("/employes/{id}/pointages") @PreAuthorize("hasAnyRole('EMPLOYE','MANAGER','RH','ADMIN')")
    public ResponseEntity<?> rechercher(@PathVariable long id,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate du,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate au,Authentication auth){
        boolean manager=hasRole(auth,"MANAGER"),privileged=hasRole(auth,"RH")||hasRole(auth,"ADMIN");
        if(!privileged)access.verifierAccesEmploye(id,auth.getName(),manager);
        return ResponseEntity.ok(service.pointages(id,du,au));
    }

    @GetMapping("/pointages/aujourdhui") @PreAuthorize("hasAnyRole('EMPLOYE','MANAGER','RH','ADMIN')")
    public ResponseEntity<?> aujourdhui(Authentication auth){return ResponseEntity.ok(service.aujourdhui(auth.getName()));}

    private static boolean hasRole(Authentication auth,String role){return auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_"+role));}
    private static String role(Authentication auth){return auth.getAuthorities().stream().map(a->a.getAuthority().replaceFirst("^ROLE_","")).findFirst().orElse(null);}
}
