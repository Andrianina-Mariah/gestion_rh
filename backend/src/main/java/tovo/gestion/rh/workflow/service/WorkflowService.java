package tovo.gestion.rh.workflow.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tovo.gestion.rh.common.exception.ResourceNotFoundException;
import tovo.gestion.rh.demanderecrutement.entity.DemandeRecrutement;
import tovo.gestion.rh.demanderecrutement.entity.StatutDemande;
import tovo.gestion.rh.demanderecrutement.repository.DemandeRecrutementRepository;
import tovo.gestion.rh.utilisateur.entity.Utilisateur;
import tovo.gestion.rh.workflow.DecisionValidation;
import tovo.gestion.rh.workflow.EtapeValidation;
import tovo.gestion.rh.workflow.TransitionInterditeException;
import tovo.gestion.rh.workflow.TransitionRegistry;
import tovo.gestion.rh.workflow.dto.ValidationHistoryItem;
import tovo.gestion.rh.workflow.entity.ValidationDemande;
import tovo.gestion.rh.workflow.repository.ValidationDemandeRepository;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final DemandeRecrutementRepository demandeRepository;
    private final ValidationDemandeRepository validationRepository;
    private final NotificationService notificationService;
    private final OuvrirRecrutementService ouvrirRecrutementService;
    private final TransitionRegistry transitionRegistry;

    @Transactional
    public DemandeRecrutement appliquerDecision(
            Integer demandeId,
            Utilisateur utilisateur,
            DecisionValidation decision,
            String commentaire,
            Boolean analyseBudget,
            BigDecimal budgetValide
    ) {
        DemandeRecrutement demande = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande de recrutement", demandeId));
        String role = utilisateur.getRole().getCode();
        EtapeValidation etape = transitionRegistry.etapePourRole(role);

        if (demande.getDemandeur().getId().equals(utilisateur.getId())) {
            throw new TransitionInterditeException(
                    "Un validateur ne peut pas valider une demande qu'il a créée"
            );
        }
        if (decision == DecisionValidation.REJETE || decision == DecisionValidation.A_CORRIGER) {
            requireComment(commentaire, decision);
        }
        if (etape == EtapeValidation.RH && decision == DecisionValidation.DEMANDE_ANALYSE
                && !Boolean.TRUE.equals(analyseBudget)) {
            throw new TransitionInterditeException(
                    "La décision DEMANDE_ANALYSE doit demander explicitement une analyse budget"
            );
        }
        if (etape == EtapeValidation.FINANCE) {
            validateFinanceDecision(demande, decision, commentaire, budgetValide);
        } else if (budgetValide != null) {
            throw new TransitionInterditeException(
                    "Seule la Finance peut renseigner un budget validé"
            );
        }

        StatutDemande statutSuivant = transitionRegistry.statutSuivant(
                demande.getStatut(), role, decision
        );
        ValidationDemande validation = ValidationDemande.builder()
                .demande(demande)
                .etape(etape)
                .validateur(utilisateur)
                .statut(decision)
                .commentaire(commentaire)
                .budgetValide(budgetValide)
                .cycle(demande.getCycle())
                .build();
        validationRepository.save(validation);

        demande.setStatut(statutSuivant);
        demande.setValidateur(utilisateur);
        notifierActeurs(demande, decision, statutSuivant);
        return demandeRepository.save(demande);
    }

    @Transactional
    public DemandeRecrutement resoumettre(Integer demandeId, Utilisateur manager) {
        DemandeRecrutement demande = getDemande(demandeId);
        if (!"MANAGER".equals(manager.getRole().getCode())
                || !demande.getDemandeur().getId().equals(manager.getId())) {
            throw new AccessDeniedException("Seul l'auteur manager peut resoumettre cette demande");
        }
        if (demande.getStatut() != StatutDemande.A_CORRIGER) {
            throw new TransitionInterditeException(
                    "Seule une demande A_CORRIGER peut être resoumise"
            );
        }
        demande.setCycle(demande.getCycle() + 1);
        demande.setStatut(StatutDemande.EN_ATTENTE_RH);
        DemandeRecrutement updated = demandeRepository.save(demande);
        notificationService.notifyRole("RH", "Demande #" + demandeId + " resoumise par le Manager.", demandeId);
        return updated;
    }

    @Transactional
    public DemandeRecrutement ouvrirRecrutement(Integer demandeId, Utilisateur rh) {
        return ouvrirRecrutementService.ouvrir(demandeId, rh);
    }

    @Transactional(readOnly = true)
    public List<ValidationHistoryItem> historique(Integer demandeId) {
        getDemande(demandeId);
        return validationRepository.findByDemandeIdOrderByDateValidationAscIdAsc(demandeId)
                .stream()
                .map(this::toHistoryItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ValidationHistoryItem> historique(Integer demandeId, Utilisateur utilisateur) {
        DemandeRecrutement demande = getDemande(demandeId);
        if ("MANAGER".equals(utilisateur.getRole().getCode())
                && !demande.getDemandeur().getId().equals(utilisateur.getId())) {
            throw new AccessDeniedException("Accès refusé à l'historique de cette demande");
        }
        return validationRepository.findByDemandeIdOrderByDateValidationAscIdAsc(demandeId)
                .stream()
                .map(this::toHistoryItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DemandeRecrutement> demandesATraiter(Utilisateur utilisateur) {
        EtapeValidation etape = transitionRegistry.etapePourRole(utilisateur.getRole().getCode());
        StatutDemande statut = switch (etape) {
            case RH -> StatutDemande.EN_ATTENTE_RH;
            case FINANCE -> StatutDemande.ANALYSE_FINANCE;
            case DIRECTION -> StatutDemande.EN_ATTENTE_DIRECTION;
        };
        return demandeRepository.findByStatutOrderByDateCreationAsc(statut);
    }

    private void validateFinanceDecision(
            DemandeRecrutement demande,
            DecisionValidation decision,
            String commentaire,
            BigDecimal budgetValide
    ) {
        if (decision == DecisionValidation.DEMANDE_ANALYSE) {
            throw new TransitionInterditeException(
                    "Finance ne peut pas demander une nouvelle analyse budget"
            );
        }
        if (decision == DecisionValidation.ACCEPTE && budgetValide == null) {
            throw new TransitionInterditeException(
                    "Finance doit renseigner le budget validé"
            );
        }
        if (budgetValide != null && budgetValide.compareTo(demande.getBudget()) != 0) {
            requireComment(commentaire, decision);
        }
    }

    private void requireComment(String commentaire, DecisionValidation decision) {
        if (commentaire == null || commentaire.isBlank()) {
            throw new TransitionInterditeException(
                    "Un commentaire est obligatoire pour la décision " + decision
            );
        }
    }

    private DemandeRecrutement getDemande(Integer demandeId) {
        return demandeRepository.findById(demandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande de recrutement", demandeId));
    }

    private ValidationHistoryItem toHistoryItem(ValidationDemande validation) {
        Utilisateur validateur = validation.getValidateur();
        return ValidationHistoryItem.builder()
                .id(validation.getId())
                .etape(validation.getEtape())
                .validateurId(validateur.getId())
                .validateurNom(validateur.getPrenom() + " " + validateur.getNom())
                .statut(validation.getStatut())
                .commentaire(validation.getCommentaire())
                .budgetValide(validation.getBudgetValide())
                .cycle(validation.getCycle())
                .dateValidation(validation.getDateValidation())
                .build();
    }

    private void notifierActeurs(
            DemandeRecrutement demande,
            DecisionValidation decision,
            StatutDemande statutSuivant
    ) {
        String message = "Demande #" + demande.getId() + " : décision " + decision
                + ", nouveau statut " + statutSuivant + ".";
        notificationService.notifyUser(demande.getDemandeur(), message, demande.getId());
        if (statutSuivant == StatutDemande.ANALYSE_FINANCE) {
            notificationService.notifyRole("FINANCE", message, demande.getId());
        } else if (statutSuivant == StatutDemande.EN_ATTENTE_DIRECTION) {
            notificationService.notifyRole("DIRECTION", message, demande.getId());
        }
    }
}
