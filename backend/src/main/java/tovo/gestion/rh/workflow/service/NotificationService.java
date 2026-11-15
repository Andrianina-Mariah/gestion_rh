package tovo.gestion.rh.workflow.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tovo.gestion.rh.common.exception.ResourceNotFoundException;
import tovo.gestion.rh.utilisateur.entity.Utilisateur;
import tovo.gestion.rh.utilisateur.repository.UtilisateurRepository;
import tovo.gestion.rh.workflow.entity.Notification;
import tovo.gestion.rh.workflow.repository.NotificationRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UtilisateurRepository utilisateurRepository;

    @Transactional
    public Notification notifyUser(Utilisateur destinataire, String message, Integer demandeId) {
        return notificationRepository.save(Notification.builder()
                .destinataire(destinataire)
                .message(message)
                .lien(demandeId == null ? null : "/demandes/" + demandeId)
                .build());
    }

    @Transactional
    public Notification notifyUserForOffer(Utilisateur destinataire, String message, Long offreId) {
        return notificationRepository.save(Notification.builder()
                .destinataire(destinataire)
                .message(message)
                .lien("/offres/" + offreId)
                .build());
    }

    @Transactional
    public Notification notifyUserForCandidature(Utilisateur destinataire, String message, Long candidatureId) {
        return notificationRepository.save(Notification.builder()
                .destinataire(destinataire)
                .message(message)
                .lien("/gestion-candidatures/" + candidatureId)
                .build());
    }

    @Transactional
    public Notification notifyUserForPath(Utilisateur destinataire, String message, String path) {
        return notificationRepository.save(Notification.builder()
                .destinataire(destinataire)
                .message(message)
                .lien(path)
                .build());
    }

    @Transactional
    public void notifyRole(String roleCode, String message, Integer demandeId) {
        utilisateurRepository.findAll().stream()
                .filter(user -> roleCode.equals(user.getRole().getCode()))
                .forEach(user -> notifyUser(user, message, demandeId));
    }

    @Transactional(readOnly = true)
    public List<Notification> findForUser(Utilisateur utilisateur) {
        return notificationRepository.findByDestinataireIdOrderByLueAscDateCreationDesc(utilisateur.getId());
    }

    @Transactional
    public Notification markRead(Integer id, Utilisateur utilisateur) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
        if (!notification.getDestinataire().getId().equals(utilisateur.getId())) {
            throw new AccessDeniedException("Cette notification ne vous est pas destinée");
        }
        notification.setLue(true);
        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Utilisateur utilisateur) {
        return notificationRepository.countByDestinataireIdAndLueFalse(utilisateur.getId());
    }
}
