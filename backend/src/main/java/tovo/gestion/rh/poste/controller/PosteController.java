package tovo.gestion.rh.poste.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tovo.gestion.rh.poste.dto.PosteRequest;
import tovo.gestion.rh.poste.dto.PosteResponse;
import tovo.gestion.rh.poste.service.PosteService;

import java.util.List;

@RestController
@RequestMapping("/api/postes")
@RequiredArgsConstructor
public class PosteController {

    private final PosteService posteService;

    @GetMapping
    public ResponseEntity<List<PosteResponse>> findAll(
            @RequestParam(required = false) Integer departementId) {
        return ResponseEntity.ok(posteService.findAll(departementId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PosteResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(posteService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    public ResponseEntity<PosteResponse> create(@Valid @RequestBody PosteRequest request) {
        PosteResponse created = posteService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    public ResponseEntity<PosteResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody PosteRequest request) {
        return ResponseEntity.ok(posteService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        posteService.delete(id);
        return ResponseEntity.noContent().build();
    }
}