package tovo.gestion.rh.embauche.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tovo.gestion.rh.candidature.entity.Candidature;
import tovo.gestion.rh.candidature.entity.CandidatureHistorique;
import tovo.gestion.rh.candidature.entity.StatutCandidature;
import tovo.gestion.rh.candidature.repository.CandidatureHistoriqueRepository;
import tovo.gestion.rh.embauche.entity.*;
import tovo.gestion.rh.embauche.repository.AffectationRepository;
import tovo.gestion.rh.embauche.repository.ContratRepository;
import tovo.gestion.rh.embauche.repository.EmployeRepository;
import tovo.gestion.rh.offreemploi.service.OffreEmploiService;
import tovo.gestion.rh.workflow.event.EmployeCreeEvent;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class CreationEmployeService {
    private final EmployeRepository employeRepository;
    private final ContratRepository contratRepository;
    private final AffectationRepository affectationRepository;
    private final OffreEmploiService offreEmploiService;
    private final ApplicationEventPublisher eventPublisher;
    private final CandidatureHistoriqueRepository historiqueRepository;

    @Transactional
    public Employe creerDepuisAcceptation(PropositionEmbauche proposition) {
        Candidature candidature = proposition.getCandidature();
        Employe employe = employeRepository.findByCandidatId(candidature.getCandidat().getId())
                .orElseGet(() -> employeRepository.save(Employe.builder()
                        .candidat(candidature.getCandidat())
                        .dateEmbauche(proposition.getDateDebut())
                        .statut(StatutEmploye.EN_ONBOARDING)
                        .build()));

        employe.setDateEmbauche(proposition.getDateDebut());
        employe.setStatut(StatutEmploye.EN_ONBOARDING);
        employe = employeRepository.save(employe);

        LocalDate dateFin = switch (proposition.getTypeContrat()) {
            case CE -> proposition.getDateDebut().plusMonths(6);
            case CDD -> proposition.getDateDebut().plusYears(2);
            case CDI -> null;
        };
        contratRepository.save(Contrat.builder()
                .employe(employe)
                .typeContrat(proposition.getTypeContrat())
                .dateDebut(proposition.getDateDebut())
                .dateFin(dateFin)
                .salaire(proposition.getSalaire())
                .statut(StatutContrat.BROUILLON)
                .build());

        var poste = candidature.getOffre().getDemandeRecrutement().getPoste();
        affectationRepository.save(Affectation.builder()
                .employe(employe)
                .poste(poste)
                .departement(poste.getDepartement())
                .dateAffectation(proposition.getDateDebut())
                .build());

        candidature.setStatut(StatutCandidature.EMBAUCHE);
        candidatureRepositorySave(candidature);
        historiqueRepository.save(CandidatureHistorique.builder()
                .candidature(candidature)
                .ancienStatut(StatutCandidature.RETENU)
                .nouveauStatut(StatutCandidature.EMBAUCHE)
                .commentaire("Employé créé après acceptation de la proposition d'embauche.")
                .date(java.time.LocalDateTime.now())
                .build());
        offreEmploiService.clôturerSiQuotaAtteint(candidature.getOffre());
        eventPublisher.publishEvent(new EmployeCreeEvent(
                employe.getId(), candidature.getCandidat().getId(), candidature.getId()));
        return employe;
    }

    private void candidatureRepositorySave(Candidature candidature) {
        candidatureRepository.save(candidature);
    }

    private final tovo.gestion.rh.candidature.repository.CandidatureRepository candidatureRepository;
}
