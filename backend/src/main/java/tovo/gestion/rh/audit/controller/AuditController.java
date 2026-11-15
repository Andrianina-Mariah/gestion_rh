package tovo.gestion.rh.audit.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tovo.gestion.rh.audit.dto.AuditResponse;
import tovo.gestion.rh.audit.service.AuditService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'AUDITEUR')")
public class AuditController {
    private final AuditService auditService;

    @GetMapping
    public ResponseEntity<List<AuditResponse>> search(
            @RequestParam(required = false) Integer utilisateurId,
            @RequestParam(required = false) String entite,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime depuis,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime jusqua) {
        return ResponseEntity.ok(auditService.search(utilisateurId, entite, action, depuis, jusqua));
    }
}
