package tovo.gestion.rh.offreemploi.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tovo.gestion.rh.offreemploi.dto.OffreEmploiResponse;
import tovo.gestion.rh.offreemploi.service.OffreEmploiService;

import java.util.List;

@RestController
@RequestMapping("/api/public/offres")
@RequiredArgsConstructor
public class OffrePubliqueController {

    private final OffreEmploiService offreService;

    @GetMapping
    public ResponseEntity<List<OffreEmploiResponse>> findPublished() {
        return ResponseEntity.ok(offreService.findPublished());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OffreEmploiResponse> findPublishedById(@PathVariable Long id) {
        OffreEmploiResponse offre = offreService.findById(id);
        if (!"PUBLIEE".equals(offre.statut())) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(offre);
    }
}
