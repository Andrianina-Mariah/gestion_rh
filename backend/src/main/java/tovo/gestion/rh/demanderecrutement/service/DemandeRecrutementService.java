package tovo.gestion.rh.demanderecrutement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tovo.gestion.rh.common.exception.ResourceNotFoundException;
import tovo.gestion.rh.demanderecrutement.dto.*;
import tovo.gestion.rh.demanderecrutement.entity.DemandeRecrutement;
import tovo.gestion.rh.demanderecrutement.entity.StatutDemande;
import tovo.gestion.rh.demanderecrutement.mapper.DemandeMapper;
import tovo.gestion.rh.demanderecrutement.repository.DemandeRecrutementRepository;
import tovo.gestion.rh.poste.entity.Poste;
import tovo.gestion.rh.poste.repository.PosteRepository;
import tovo.gestion.rh.utilisateur.entity.Utilisateur;
import tovo.gestion.rh.utilisateur.repository.UtilisateurRepository;
import tovo.gestion.rh.workflow.dto.ValidationHistoryItem;
import tovo.gestion.rh.workflow.service.WorkflowService;
import tovo.gestion.rh.workflow.service.NotificationService;

@Service
@RequiredArgsConstructor
@Transactional
public class DemandeRecrutementService {

    private final DemandeRecrutementRepository demandeRepository;
    private final PosteRepository posteRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final DemandeMapper demandeMapper;
    private final WorkflowService workflowService;
    private final NotificationService notificationService;

    private Utilisateur getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
    }

    public DemandeResponse create(DemandeCreateRequest request) {
        Utilisateur currentUser = getCurrentUser();

        // RG1 : seul un MANAGER peut créer
        if (!"MANAGER".equals(currentUser.getRole().getCode())) {
            throw new AccessDeniedException("Seul un manager peut créer une demande de recrutement");
        }

        Poste poste = posteRepository.findById(request.getPosteId())
                .orElseThrow(() -> new ResourceNotFoundException("Poste", request.getPosteId()));

        // RG6 : à implémenter plus tard (lien manager ↔ département)

        DemandeRecrutement demande = demandeMapper.toEntity(request, poste, currentUser);
        DemandeRecrutement saved = demandeRepository.save(demande);
        return demandeMapper.toResponse(saved);
    }

    public DemandeResponse update(Integer id, DemandeUpdateRequest request) {
        Utilisateur currentUser = getCurrentUser();
        DemandeRecrutement demande = getDemandeOrThrow(id);

        // RG7 : seul l'auteur peut modifier un brouillon
        if (!demande.getDemandeur().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Vous ne pouvez modifier que vos propres demandes");
        }

        // Un manager peut modifier son brouillon ou corriger une demande renvoyée.
        // Le cycle de validation n'est incrémenté qu'à la resoumission dans WorkflowService.
        if (demande.getStatut() != StatutDemande.BROUILLON
                && demande.getStatut() != StatutDemande.A_CORRIGER) {
            throw new IllegalStateException("Seules les demandes en brouillon ou à corriger peuvent être modifiées");
        }

        Poste poste = posteRepository.findById(request.getPosteId())
                .orElseThrow(() -> new ResourceNotFoundException("Poste", request.getPosteId()));

        demandeMapper.updateEntity(demande, request, poste);
        DemandeRecrutement updated = demandeRepository.save(demande);
        return demandeMapper.toResponse(updated);
    }

    public void delete(Integer id) {
        Utilisateur currentUser = getCurrentUser();
        DemandeRecrutement demande = getDemandeOrThrow(id);

        if (!demande.getDemandeur().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Vous ne pouvez supprimer que vos propres demandes");
        }

        if (demande.getStatut() != StatutDemande.BROUILLON) {
            throw new IllegalStateException("Seules les demandes en brouillon peuvent être supprimées");
        }

        demandeRepository.delete(demande);
    }

    public DemandeResponse soumettre(Integer id) {
        Utilisateur currentUser = getCurrentUser();
        DemandeRecrutement demande = getDemandeOrThrow(id);

        if (!demande.getDemandeur().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Vous ne pouvez soumettre que vos propres demandes");
        }

        if (demande.getStatut() != StatutDemande.BROUILLON) {
            throw new IllegalStateException("Seules les demandes en brouillon peuvent être soumises");
        }

        demande.setStatut(StatutDemande.EN_ATTENTE_RH);
        DemandeRecrutement saved = demandeRepository.save(demande);
        notificationService.notifyRole(
                "RH",
                "La demande #" + id + " a été soumise et attend une décision RH.",
                id
        );
        return demandeMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public DemandeResponse findById(Integer id) {
        Utilisateur currentUser = getCurrentUser();
        DemandeRecrutement demande = getDemandeOrThrow(id);

        // RG9 : un manager ne voit que ses demandes
        if ("MANAGER".equals(currentUser.getRole().getCode())
                && !demande.getDemandeur().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Accès refusé à cette demande");
        }

        DemandeResponse response = demandeMapper.toResponse(demande);
        response.setHistorique(workflowService.historique(id));
        return response;
    }

    @Transactional(readOnly = true)
    public Page<DemandeListItem> search(StatutDemande statut, Integer departementId, Pageable pageable) {
        Utilisateur currentUser = getCurrentUser();
        String role = currentUser.getRole().getCode();

        Integer demandeurId = null;

        // RG9 : le manager ne voit que ses demandes
        if ("MANAGER".equals(role)) {
            demandeurId = currentUser.getId();
        }

        return demandeRepository.search(statut, demandeurId, departementId, pageable)
                .map(demandeMapper::toListItem);
    }

    private DemandeRecrutement getDemandeOrThrow(Integer id) {
        return demandeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demande de recrutement", id));
    }
}
