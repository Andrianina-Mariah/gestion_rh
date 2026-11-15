package tovo.gestion.rh.candidature.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tovo.gestion.rh.candidature.dto.*;
import tovo.gestion.rh.candidature.entity.*;
import tovo.gestion.rh.candidature.repository.CandidatureHistoriqueRepository;
import tovo.gestion.rh.candidature.repository.CandidatureRepository;
import tovo.gestion.rh.common.exception.ResourceNotFoundException;
import tovo.gestion.rh.utilisateur.entity.Utilisateur;
import tovo.gestion.rh.offreemploi.repository.OffreEmploiRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SelectionDossierService {
    private final CandidatureRepository candidatureRepository;
    private final CandidatureHistoriqueRepository historiqueRepository;
    private final OffreEmploiRepository offreRepository;

    @Transactional
    public CandidatureResponse decider(Long candidatureId, DecisionDossierRequest request,
                                       Utilisateur utilisateur) {
        requireRh(utilisateur);
        Candidature candidature = getCandidature(candidatureId);
        StatutCandidature ancienStatut = candidature.getStatut();
        if (ancienStatut != StatutCandidature.RECU
                && !(ancienStatut == StatutCandidature.EN_ATTENTE
                && request.decision() == DecisionDossier.REJETER)) {
            throw new IllegalStateException("Cette candidature ne peut plus faire l'objet d'une décision de dossier");
        }
        if (request.decision() == DecisionDossier.REJETER
                && (request.commentaire() == null || request.commentaire().isBlank())) {
            throw new IllegalArgumentException("Un commentaire est obligatoire pour rejeter une candidature");
        }

        StatutCandidature nouveauStatut = switch (request.decision()) {
            case ACCEPTER -> StatutCandidature.PRESELECTIONNE;
            case LISTE_ATTENTE -> StatutCandidature.EN_ATTENTE;
            case REJETER -> StatutCandidature.REJETE;
        };
        candidature.setStatut(nouveauStatut);
        if (nouveauStatut == StatutCandidature.EN_ATTENTE) {
            candidature.setRangAttente(nextRank(candidature.getOffre().getId()));
        } else {
            candidature.setRangAttente(null);
        }
        candidatureRepository.save(candidature);
        saveHistory(candidature, ancienStatut, nouveauStatut, utilisateur, request.commentaire());
        return CandidatureResponse.from(candidature, historique(candidatureId));
    }

    @Transactional
    public CandidatureResponse promouvoir(Long candidatureId, Utilisateur utilisateur) {
        requireRh(utilisateur);
        Candidature candidature = getCandidature(candidatureId);
        if (candidature.getStatut() != StatutCandidature.EN_ATTENTE) {
            throw new IllegalStateException("Seule une candidature en liste d'attente peut être promue");
        }
        StatutCandidature ancienStatut = candidature.getStatut();
        candidature.setStatut(StatutCandidature.PRESELECTIONNE);
        candidature.setRangAttente(null);
        candidatureRepository.save(candidature);
        saveHistory(candidature, ancienStatut, candidature.getStatut(), utilisateur,
                "Candidature promue depuis la liste d'attente");
        return CandidatureResponse.from(candidature, historique(candidatureId));
    }

    @Transactional(readOnly = true)
    public List<CandidatureResponse> listeAttente(Long offreId, Utilisateur utilisateur) {
        var offre = offreRepository.findById(offreId)
                .orElseThrow(() -> new ResourceNotFoundException("Offre d'emploi", offreId));
        if (!"RH".equals(utilisateur.getRole().getCode())
                && !"ADMIN".equals(utilisateur.getRole().getCode())
                && !( "MANAGER".equals(utilisateur.getRole().getCode())
                && offre.getDemandeRecrutement().getDemandeur().getId().equals(utilisateur.getId()))) {
            throw new AccessDeniedException("Vous ne pouvez pas consulter cette liste d'attente");
        }
        return candidatureRepository
                .findByOffreIdAndStatutOrderByRangAttenteAscDateCandidatureAsc(
                        offreId, StatutCandidature.EN_ATTENTE)
                .stream()
                .map(c -> CandidatureResponse.from(c, historique(c.getId())))
                .toList();
    }

    private int nextRank(Long offreId) {
        Integer maximum = candidatureRepository
                .findMaxRangAttente(
                        offreId, StatutCandidature.EN_ATTENTE);
        return maximum == null ? 1 : maximum + 1;
    }

    private void saveHistory(Candidature candidature, StatutCandidature ancien,
                             StatutCandidature nouveau, Utilisateur utilisateur,
                             String commentaire) {
        historiqueRepository.save(CandidatureHistorique.builder()
                .candidature(candidature)
                .ancienStatut(ancien)
                .nouveauStatut(nouveau)
                .utilisateur(utilisateur)
                .commentaire(commentaire)
                .date(LocalDateTime.now())
                .build());
    }

    private List<HistoriqueResponse> historique(Long candidatureId) {
        return historiqueRepository.findByCandidatureIdOrderByDateAsc(candidatureId)
                .stream().map(HistoriqueResponse::from).toList();
    }

    private Candidature getCandidature(Long id) {
        return candidatureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidature", id));
    }

    private void requireRh(Utilisateur utilisateur) {
        if (!"RH".equals(utilisateur.getRole().getCode())) {
            throw new AccessDeniedException("Seule la RH peut décider de la sélection des dossiers");
        }
    }
}
