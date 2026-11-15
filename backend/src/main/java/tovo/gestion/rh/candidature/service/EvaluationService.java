package tovo.gestion.rh.candidature.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import tovo.gestion.rh.candidature.dto.*;
import tovo.gestion.rh.candidature.entity.*;
import tovo.gestion.rh.candidature.repository.*;
import tovo.gestion.rh.common.exception.ResourceNotFoundException;
import tovo.gestion.rh.utilisateur.entity.Utilisateur;
import tovo.gestion.rh.utilisateur.repository.UtilisateurRepository;
import tovo.gestion.rh.offreemploi.service.OffreEmploiService;
import tovo.gestion.rh.workflow.event.CandidatRetenuEvent;
import tovo.gestion.rh.workflow.service.NotificationService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EvaluationService {
    private final EvaluationRepository evaluationRepository;
    private final CandidatureRepository candidatureRepository;
    private final CandidatureHistoriqueRepository historiqueRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final NotificationService notificationService;
    private final OffreEmploiService offreEmploiService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public EvaluationResponse planifier(Long candidatureId, PlanifierEvaluationRequest request,
                                         Utilisateur rh) {
        Candidature candidature = getCandidature(candidatureId);
        requireCanEvaluate(candidature, rh);
        if (request.type() == TypeEvaluation.TEST_TECHNIQUE
                && candidature.getStatut() != StatutCandidature.PRESELECTIONNE) {
            throw new IllegalStateException("Un test ne peut être planifié que pour une candidature PRESELECTIONNEE");
        }
        if (request.type() != TypeEvaluation.TEST_TECHNIQUE
                && candidature.getStatut() != StatutCandidature.TEST_PASSE) {
            throw new IllegalStateException("Un entretien ne peut être planifié qu'après un test accepté");
        }
        Utilisateur evaluateur = utilisateurRepository.findById(request.evaluateurId().longValue())
                .orElseThrow(() -> new ResourceNotFoundException("Evaluateur", request.evaluateurId()));
        if (evaluationRepository.existsByEvaluateurAndDatePlanifiee(evaluateur, request.date())) {
            throw new IllegalStateException("L'évaluateur a déjà une évaluation planifiée à cette date");
        }
        Evaluation evaluation = Evaluation.builder()
                .candidature(candidature).type(request.type()).datePlanifiee(request.date())
                .evaluateur(evaluateur).build();
        EvaluationResponse response = EvaluationResponse.from(evaluationRepository.save(evaluation));
        notificationService.notifyUserForCandidature(evaluateur,
                "Une évaluation vous a été planifiée pour la candidature #" + candidatureId + ".",
                candidatureId);
        return response;
    }

    @Transactional
    public EvaluationResponse saisirResultat(Long evaluationId, ResultatEvaluationRequest request,
                                              Utilisateur rh) {
        Evaluation evaluation = getEvaluation(evaluationId);
        requireCanEvaluate(evaluation.getCandidature(), rh);
        if (evaluation.getResultat() != null) {
            throw new IllegalStateException("Le résultat est définitif ; créez une nouvelle évaluation pour une correction");
        }
        if (request.note() != null && (request.note().signum() < 0
                || request.note().doubleValue() > 20)) {
            throw new IllegalArgumentException("La note doit être comprise entre 0 et 20");
        }
        if (request.date().isBefore(evaluation.getDatePlanifiee())) {
            throw new IllegalArgumentException("La date de réalisation ne peut pas précéder la date planifiée");
        }
        evaluation.setDateRealisation(request.date());
        evaluation.setNote(request.note());
        evaluation.setRemarque(request.remarque());
        evaluation.setResultat(request.resultat());
        Candidature candidature = evaluation.getCandidature();
        StatutCandidature ancien = candidature.getStatut();
        if (request.resultat() == ResultatEvaluation.REJETE) {
            candidature.setStatut(StatutCandidature.REJETE);
        } else if (request.resultat() == ResultatEvaluation.LISTE_ATTENTE) {
            candidature.setStatut(StatutCandidature.EN_ATTENTE);
        } else if (evaluation.getType() == TypeEvaluation.TEST_TECHNIQUE) {
            candidature.setStatut(StatutCandidature.TEST_PASSE);
        } else {
            candidature.setStatut(StatutCandidature.ENTRETIEN_PASSE);
        }
        candidatureRepository.save(candidature);
        historiqueRepository.save(CandidatureHistorique.builder()
                .candidature(candidature).ancienStatut(ancien)
                .nouveauStatut(candidature.getStatut()).utilisateur(rh)
                .commentaire(request.remarque()).date(LocalDateTime.now()).build());
        EvaluationResponse response = EvaluationResponse.from(evaluationRepository.save(evaluation));
        notificationService.notifyRole("RH",
                "Le résultat de l'évaluation #" + evaluationId + " a été saisi.",
                evaluation.getCandidature().getOffre().getDemandeRecrutement().getId());
        return response;
    }

    @Transactional
    public CandidatureResponse decisionFinale(Long candidatureId, DecisionFinaleRequest request,
                                               Utilisateur rh) {
        Candidature candidature = getCandidature(candidatureId);
        requireCanEvaluate(candidature, rh);
        if (candidature.getStatut() != StatutCandidature.ENTRETIEN_PASSE
                && !(request.decision() == DecisionFinaleRequest.DecisionFinale.REJETE
                && candidature.getStatut() == StatutCandidature.TEST_PASSE)) {
            throw new IllegalStateException("La décision finale intervient après le test ou l'entretien");
        }
        StatutCandidature ancien = candidature.getStatut();
        candidature.setStatut(request.decision() == DecisionFinaleRequest.DecisionFinale.RETENU
                ? StatutCandidature.RETENU : StatutCandidature.REJETE);
        candidatureRepository.save(candidature);
        historiqueRepository.save(CandidatureHistorique.builder()
                .candidature(candidature).ancienStatut(ancien)
                .nouveauStatut(candidature.getStatut()).utilisateur(rh)
                .commentaire(request.commentaire()).date(LocalDateTime.now()).build());
        if (candidature.getStatut() == StatutCandidature.RETENU) {
            eventPublisher.publishEvent(new CandidatRetenuEvent(
                    candidature.getId(), candidature.getOffre().getId()));
            offreEmploiService.clôturerSiQuotaAtteint(candidature.getOffre());
        }
        return CandidatureResponse.from(candidature,
                historiqueRepository.findByCandidatureIdOrderByDateAsc(candidatureId)
                        .stream().map(HistoriqueResponse::from).toList());
    }

    @Transactional(readOnly = true)
    public List<EvaluationResponse> findByCandidature(Long candidatureId, Utilisateur utilisateur) {
        Candidature candidature = getCandidature(candidatureId);
        requireCanEvaluate(candidature, utilisateur);
        return evaluationRepository.findByCandidatureIdOrderByDatePlanifieeAsc(candidatureId)
                .stream().map(EvaluationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<CandidatureResponse> retenus(Long offreId, Utilisateur utilisateur) {
        var offre = candidatureRepository.findByOffreIdAndStatutOrderByRangAttenteAscDateCandidatureAsc(
                offreId, StatutCandidature.RETENU);
        if (!offre.isEmpty()) {
            requireCanEvaluate(offre.get(0), utilisateur);
        }
        return candidatureRepository.findByOffreIdAndStatutOrderByRangAttenteAscDateCandidatureAsc(
                        offreId, StatutCandidature.RETENU)
                .stream()
                .map(c -> CandidatureResponse.from(c,
                        historiqueRepository.findByCandidatureIdOrderByDateAsc(c.getId())
                                .stream().map(HistoriqueResponse::from).toList()))
                .toList();
    }

    private Candidature getCandidature(Long id) {
        return candidatureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidature", id));
    }
    private Evaluation getEvaluation(Long id) {
        return evaluationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluation", id));
    }
    private void requireCanEvaluate(Candidature candidature, Utilisateur utilisateur) {
        if ("RH".equals(utilisateur.getRole().getCode())
                || "ADMIN".equals(utilisateur.getRole().getCode())) {
            return;
        }
        if ("MANAGER".equals(utilisateur.getRole().getCode())
                && candidature.getOffre().getDemandeRecrutement().getDemandeur()
                .getId().equals(utilisateur.getId())) {
            return;
        }
        throw new AccessDeniedException("Vous ne pouvez pas évaluer cette candidature");
    }
}
