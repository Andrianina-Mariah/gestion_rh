package tovo.gestion.rh.departement.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tovo.gestion.rh.departement.dto.DepartementRequest;
import tovo.gestion.rh.departement.dto.DepartementResponse;
import tovo.gestion.rh.departement.service.DepartementService;

import java.util.List;

@RestController
@RequestMapping("/api/departements")
@RequiredArgsConstructor
public class DepartementController {

    private final DepartementService departementService;

    @GetMapping
    public ResponseEntity<List<DepartementResponse>> findAll() {
        return ResponseEntity.ok(departementService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepartementResponse> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(departementService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    public ResponseEntity<DepartementResponse> create(@Valid @RequestBody DepartementRequest request) {
        DepartementResponse created = departementService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    public ResponseEntity<DepartementResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody DepartementRequest request) {
        return ResponseEntity.ok(departementService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('RH', 'ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        departementService.delete(id);
        return ResponseEntity.noContent().build();
    }
}