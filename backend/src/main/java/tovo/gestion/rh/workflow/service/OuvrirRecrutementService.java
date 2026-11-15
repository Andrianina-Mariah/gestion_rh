package tovo.gestion.rh.workflow.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tovo.gestion.rh.common.exception.ResourceNotFoundException;
import tovo.gestion.rh.demanderecrutement.entity.DemandeRecrutement;
import tovo.gestion.rh.demanderecrutement.entity.StatutDemande;
import tovo.gestion.rh.demanderecrutement.repository.DemandeRecrutementRepository;
import tovo.gestion.rh.utilisateur.entity.Utilisateur;
import tovo.gestion.rh.workflow.event.RecrutementOuvertEvent;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OuvrirRecrutementService {

    private final DemandeRecrutementRepository demandeRepository;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public DemandeRecrutement ouvrir(Integer demandeId, Utilisateur rh) {
        if (!"RH".equals(rh.getRole().getCode())) {
            throw new AccessDeniedException("Seule la RH peut ouvrir un recrutement");
        }

        DemandeRecrutement demande = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande de recrutement", demandeId));
        if (demande.getStatut() != StatutDemande.VALIDE) {
            throw new IllegalStateException("Seule une demande VALIDE peut ouvrir le recrutement");
        }

        demande.setStatut(StatutDemande.RECRUTEMENT_OUVERT);
        demande.setDateOuvertureRecrutement(LocalDateTime.now());
        demande.setOuvertPar(rh);
        DemandeRecrutement saved = demandeRepository.save(demande);

        notificationService.notifyUser(
                demande.getDemandeur(),
                "Le recrutement de la demande #" + demandeId + " est ouvert.",
                demandeId
        );
        eventPublisher.publishEvent(new RecrutementOuvertEvent(demandeId, rh.getId()));
        return saved;
    }
}
