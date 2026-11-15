package tovo.gestion.rh.temps.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tovo.gestion.rh.temps.dto.ReouverturePeriodeRequest;
import tovo.gestion.rh.temps.dto.CreerPeriodeRequest;
import tovo.gestion.rh.temps.dto.ValidationPeriodeRequest;
import tovo.gestion.rh.temps.service.PeriodeTempsService;
import java.util.Map;

@RestController @RequestMapping("/api/periodes") @RequiredArgsConstructor
public class PeriodeTempsController {
    private final PeriodeTempsService service;

    @GetMapping @PreAuthorize("hasAnyRole('RH','ADMIN')")
    public ResponseEntity<?> lister(){return ResponseEntity.ok(service.lister());}
    @PostMapping @PreAuthorize("hasAnyRole('RH','ADMIN')")
    public ResponseEntity<?> creer(@Valid @RequestBody CreerPeriodeRequest request){return ResponseEntity.ok(service.creer(request));}

    @PostMapping("/{id}/valider") @PreAuthorize("hasAnyRole('RH','ADMIN')")
    public ResponseEntity<?> valider(@PathVariable long id,@Valid @RequestBody(required=false) ValidationPeriodeRequest request,Authentication auth){return ResponseEntity.ok(service.valider(id,role(auth),service.utilisateurId(auth.getName()),request));}

    @GetMapping("/{id}/variables-paie") @PreAuthorize("hasAnyRole('RH','ADMIN')")
    public ResponseEntity<?> exporter(@PathVariable long id,@RequestParam(defaultValue="json") String format,Authentication auth){
        var result=service.exporter(id,format,role(auth),service.utilisateurId(auth.getName()));
        if(result.csv()!=null)return ResponseEntity.ok().contentType(MediaType.parseMediaType(result.contentType())).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=variables-paie-"+id+".csv").body(result.csv());
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(result.json());
    }

    @PostMapping("/{id}/reouvrir") @PreAuthorize("hasAnyRole('RH','ADMIN')")
    public ResponseEntity<?> reouvrir(@PathVariable long id,@Valid @RequestBody ReouverturePeriodeRequest request,Authentication auth){return ResponseEntity.ok(service.reouvrir(id,role(auth),service.utilisateurId(auth.getName()),request));}
    private static String role(Authentication a){return a.getAuthorities().stream().map(x->x.getAuthority().replaceFirst("^ROLE_","")).findFirst().orElse("");}
}
