package tovo.gestion.rh.conge.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tovo.gestion.rh.conge.dto.*;
import tovo.gestion.rh.conge.service.AbsenceService;
import tovo.gestion.rh.temps.service.TempsPlanningService;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Set;

@RestController @RequestMapping("/api") @RequiredArgsConstructor
public class AbsenceController {
    private final AbsenceService service;
    private final TempsPlanningService access;

    @PostMapping("/absences") @PreAuthorize("hasAnyRole('RH','ADMIN','MANAGER')")
    public ResponseEntity<?> creer(@Valid @RequestBody AbsenceRequest r,Authentication auth){return ResponseEntity.ok(service.creer(r,auth.getName(),role(auth)));}
    @PostMapping("/absences/{id}/qualifier") @PreAuthorize("hasAnyRole('RH','ADMIN','MANAGER')")
    public ResponseEntity<?> qualifier(@PathVariable long id,@Valid @RequestBody QualificationAbsenceRequest r,Authentication auth){return ResponseEntity.ok(service.qualifier(id,r,auth.getName(),role(auth)));}
    @PostMapping(value="/absences/{id}/justificatifs",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @PreAuthorize("hasAnyRole('RH','ADMIN','MANAGER','EMPLOYE')")
    public ResponseEntity<?> deposer(@PathVariable long id,@RequestPart("fichier") MultipartFile file,Authentication auth)throws IOException{return ResponseEntity.ok(java.util.Map.of("id",service.deposerJustificatif(id,file,auth.getName(),role(auth))));}
    @GetMapping("/absences/{id}/justificatifs") @PreAuthorize("hasAnyRole('RH','ADMIN','MANAGER','EMPLOYE')")
    public ResponseEntity<?> justificatifsAbsence(@PathVariable long id,Authentication auth){authorize(service.employeIdAbsence(id),auth);return ResponseEntity.ok(service.justificatifs(id,null));}
    @GetMapping("/justificatifs") @PreAuthorize("hasAnyRole('RH','ADMIN')")
    public ResponseEntity<?> justificatifs(@RequestParam(required=false) String statut){if(statut!=null&&!Set.of("DEPOSE","VALIDE","REJETE").contains(statut))throw new IllegalArgumentException("Statut de justificatif invalide");return ResponseEntity.ok(service.justificatifs(null,statut));}
    @PostMapping("/justificatifs/{id}/valider") @PreAuthorize("hasAnyRole('RH','ADMIN')")
    public ResponseEntity<?> valider(@PathVariable long id,Authentication auth){return ResponseEntity.ok(service.traiterJustificatif(id,true,null,auth.getName()));}
    @PostMapping("/justificatifs/{id}/rejeter") @PreAuthorize("hasAnyRole('RH','ADMIN')")
    public ResponseEntity<?> rejeter(@PathVariable long id,@Valid @RequestBody JustificatifDecisionRequest r,Authentication auth){return ResponseEntity.ok(service.traiterJustificatif(id,false,r.motif(),auth.getName()));}
    @GetMapping("/employes/{id}/absences") @PreAuthorize("hasAnyRole('RH','ADMIN','MANAGER','EMPLOYE')")
    public ResponseEntity<?> absences(@PathVariable long id,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate du,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate au,Authentication auth){if(du!=null&&au!=null&&au.isBefore(du))throw new IllegalArgumentException("Période invalide");authorize(id,auth);return ResponseEntity.ok(service.absences(id,du,au));}
    @GetMapping("/moi/absences") @PreAuthorize("hasAnyRole('RH','ADMIN','MANAGER','EMPLOYE')")
    public ResponseEntity<?> mesAbsences(@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate du,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate au,Authentication auth){long id=service.employeConnecte(auth.getName());return ResponseEntity.ok(service.absences(id,du,au));}
    @GetMapping("/absences") @PreAuthorize("hasAnyRole('RH','ADMIN','MANAGER','EMPLOYE')")
    public ResponseEntity<?> rechercher(@RequestParam(required=false) String statut,@RequestParam(required=false) String type,
          @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate du,
          @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate au,
          @RequestParam(required=false) Integer departement,Authentication auth){
        if(statut!=null&&!Set.of("CONSTATEE","JUSTIFIEE","NON_JUSTIFIEE").contains(statut))throw new IllegalArgumentException("Statut d'absence invalide");
        if(type!=null&&!Set.of("MALADIE","INJUSTIFIEE","EVENEMENT_FAMILIAL","MISSION","AUTRE").contains(type))throw new IllegalArgumentException("Type d'absence invalide");
        if(du!=null&&au!=null&&au.isBefore(du))throw new IllegalArgumentException("Période invalide");
        return ResponseEntity.ok(service.rechercher(auth.getName(),role(auth),statut,type,du,au,departement));
    }
    @GetMapping("/rapports/absenteisme") @PreAuthorize("hasAnyRole('RH','ADMIN')")
    public ResponseEntity<?> rapport(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate du,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate au,@RequestParam(required=false) Integer departement){return ResponseEntity.ok(service.rapport(du,au,departement));}
    @GetMapping("/politique-absences") @PreAuthorize("hasAnyRole('RH','ADMIN')") public ResponseEntity<?> politique(){return ResponseEntity.ok(service.politique());}
    @PutMapping("/politique-absences") @PreAuthorize("hasAnyRole('RH','ADMIN')") public ResponseEntity<?> politique(@Valid @RequestBody PolitiqueAbsenceRequest r){return ResponseEntity.ok(service.modifierPolitique(r.delaiJustificatifJours(),r.tailleMaxJustificatifOctets()));}

    private void authorize(long id,Authentication auth){if(has(auth,"RH")||has(auth,"ADMIN"))return;access.verifierAccesEmploye(id,auth.getName(),has(auth,"MANAGER"));}
    private String role(Authentication auth){return auth.getAuthorities().stream().map(a->a.getAuthority()).filter(a->a.startsWith("ROLE_")).map(a->a.substring(5)).findFirst().orElse("");}
    private boolean has(Authentication auth,String role){return auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_"+role));}
}
