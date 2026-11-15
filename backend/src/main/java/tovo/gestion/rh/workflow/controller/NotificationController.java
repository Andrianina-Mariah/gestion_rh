package tovo.gestion.rh.workflow.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tovo.gestion.rh.utilisateur.entity.Utilisateur;
import tovo.gestion.rh.utilisateur.repository.UtilisateurRepository;
import tovo.gestion.rh.workflow.dto.NotificationResponse;
import tovo.gestion.rh.workflow.entity.Notification;
import tovo.gestion.rh.workflow.service.NotificationService;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UtilisateurRepository utilisateurRepository;

    @Operation(summary = "Lister les notifications de l'utilisateur connecté")
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> findAll(Authentication authentication) {
        return ResponseEntity.ok(notificationService.findForUser(currentUser(authentication))
                .stream().map(this::toResponse).toList());
    }

    @Operation(summary = "Marquer une notification comme lue")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification marquée comme lue"),
            @ApiResponse(responseCode = "403", description = "Notification appartenant à un autre utilisateur"),
            @ApiResponse(responseCode = "404", description = "Notification introuvable")
    })
    @PatchMapping("/{id}/lue")
    public ResponseEntity<NotificationResponse> markRead(
            @PathVariable Integer id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(toResponse(
                notificationService.markRead(id, currentUser(authentication))
        ));
    }

    @Operation(summary = "Compter les notifications non lues")
    @GetMapping("/non-lues/count")
    public ResponseEntity<Long> unreadCount(Authentication authentication) {
        return ResponseEntity.ok(notificationService.unreadCount(currentUser(authentication)));
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .message(notification.getMessage())
                .lien(notification.getLien())
                .lue(notification.getLue())
                .dateCreation(notification.getDateCreation())
                .build();
    }

    private Utilisateur currentUser(Authentication authentication) {
        return utilisateurRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Utilisateur authentifié introuvable"));
    }
}
