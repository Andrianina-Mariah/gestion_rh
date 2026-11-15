package tovo.gestion.rh.offreemploi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tovo.gestion.rh.candidature.entity.Candidature;
import tovo.gestion.rh.candidature.entity.StatutCandidature;
import tovo.gestion.rh.candidature.entity.CandidatureHistorique;
import tovo.gestion.rh.candidature.repository.CandidatureRepository;
import tovo.gestion.rh.candidature.repository.CandidatureHistoriqueRepository;
import tovo.gestion.rh.common.exception.ResourceNotFoundException;
import tovo.gestion.rh.demanderecrutement.entity.DemandeRecrutement;
import tovo.gestion.rh.demanderecrutement.entity.StatutDemande;
import tovo.gestion.rh.demanderecrutement.repository.DemandeRecrutementRepository;
import tovo.gestion.rh.offreemploi.dto.OffreEmploiRequest;
import tovo.gestion.rh.offreemploi.dto.OffreEmploiResponse;
import tovo.gestion.rh.offreemploi.entity.OffreEmploi;
import tovo.gestion.rh.offreemploi.entity.StatutOffre;
import tovo.gestion.rh.offreemploi.repository.OffreEmploiRepository;
import tovo.gestion.rh.utilisateur.entity.Utilisateur;
import tovo.gestion.rh.workflow.event.RecrutementOuvertEvent;
import tovo.gestion.rh.workflow.service.NotificationService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OffreEmploiService {

    private final OffreEmploiRepository offreRepository;
    private final DemandeRecrutementRepository demandeRepository;
    private final CandidatureRepository candidatureRepository;
    private final NotificationService notificationService;
    private final CandidatureHistoriqueRepository historiqueRepository;

    @EventListener
    @Transactional
    public void creerDepuisRecrutementOuvert(RecrutementOuvertEvent event) {
        if (offreRepository.findFirstByDemandeRecrutementIdOrderByIdAsc(event.demandeId()).isPresent()) {
            return;
        }

        DemandeRecrutement demande = getDemande(event.demandeId());
        if (demande.getStatut() != StatutDemande.RECRUTEMENT_OUVERT) {
            throw new IllegalStateException("Une offre ne peut être créée que pour une demande RECRUTEMENT_OUVERT");
        }

        String titre = demande.getPoste().getTitre();
        String profil = demande.getPoste().getDescriptionProfil();
        offreRepository.save(OffreEmploi.builder()
                .demandeRecrutement(demande)
                .titreOffre(titre)
                .descriptionPoste(profil == null ? titre : profil)
                .profilRecherche(profil == null ? titre : profil)
                .lieuTravail("À définir")
                .statut(StatutOffre.BROUILLON)
                .build());
    }

    @Transactional(readOnly = true)
    public List<OffreEmploiResponse> findAll(Utilisateur utilisateur) {
        if ("MANAGER".equals(utilisateur.getRole().getCode())) {
            return offreRepository.findByDemandeRecrutementDemandeurIdOrderByIdDesc(utilisateur.getId()).stream()
                    .map(OffreEmploiResponse::from).toList();
        }
        return offreRepository.findAllByOrderByIdDesc().stream()
                .map(OffreEmploiResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OffreEmploiResponse> findPublished() {
        return offreRepository.findByStatutOrderByDatePublicationDesc(StatutOffre.PUBLIEE).stream()
                .map(OffreEmploiResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OffreEmploiResponse findById(Long id, Utilisateur utilisateur) {
        OffreEmploi offre = getOffre(id);
        if ("MANAGER".equals(utilisateur.getRole().getCode()) && !accessibleParManager(offre, utilisateur)) {
            throw new AccessDeniedException("Vous ne pouvez pas consulter cette offre");
        }
        return OffreEmploiResponse.from(offre);
    }

    @Transactional(readOnly = true)
    public OffreEmploiResponse findById(Long id) {
        return OffreEmploiResponse.from(getOffre(id));
    }

    @Transactional
    public OffreEmploiResponse update(Long id, OffreEmploiRequest request, Utilisateur rh) {
        requireRh(rh);
        OffreEmploi offre = getOffre(id);
        if (offre.getStatut() != StatutOffre.BROUILLON) {
            throw new IllegalStateException("Seule une offre BROUILLON peut être modifiée");
        }

        offre.setTitreOffre(request.titreOffre());
        offre.setDescriptionPoste(request.descriptionPoste());
        offre.setProfilRecherche(request.profilRecherche());
        offre.setLieuTravail(request.lieuTravail());
        offre.setTypeContratPropose(request.typeContratPropose());
        return OffreEmploiResponse.from(offreRepository.save(offre));
    }

    @Transactional
    public OffreEmploiResponse publier(Long id, Utilisateur rh) {
        requireRh(rh);
        OffreEmploi offre = getOffre(id);
        requireDemandeOuverte(offre);
        if (offre.getStatut() != StatutOffre.BROUILLON) {
            throw new IllegalStateException("Seule une offre BROUILLON peut être publiée");
        }

        offre.setStatut(StatutOffre.PUBLIEE);
        offre.setDatePublication(LocalDateTime.now());
        return OffreEmploiResponse.from(offreRepository.save(offre));
    }

    @Transactional
    public OffreEmploiResponse cloturer(Long id, Utilisateur rh) {
        requireRh(rh);
        OffreEmploi offre = getOffre(id);
        if (offre.getStatut() != StatutOffre.PUBLIEE) {
            throw new IllegalStateException("Seule une offre PUBLIEE peut être clôturée");
        }

        offre.setStatut(StatutOffre.CLOTUREE);
        offre.setDateCloture(LocalDateTime.now());
        clôturerCandidatures(offre);
        return OffreEmploiResponse.from(offreRepository.save(offre));
    }

    @Transactional
    public void clôturerSiQuotaAtteint(OffreEmploi offre) {
        long retenus = candidatureRepository.countByOffreIdAndStatut(
                offre.getId(), StatutCandidature.RETENU);
        long embauches = candidatureRepository.countByOffreIdAndStatut(
                offre.getId(), StatutCandidature.EMBAUCHE);
        if (offre.getStatut() == StatutOffre.PUBLIEE
                && retenus + embauches >= offre.getDemandeRecrutement().getNombrePostes()) {
            offre.setStatut(StatutOffre.CLOTUREE);
            offre.setDateCloture(LocalDateTime.now());
            clôturerCandidatures(offre);
            offreRepository.save(offre);
        }
    }

    @Transactional(readOnly = true)
    public boolean accessibleParManager(OffreEmploi offre, Utilisateur utilisateur) {
        return "MANAGER".equals(utilisateur.getRole().getCode())
                && offre.getDemandeRecrutement().getDemandeur().getId().equals(utilisateur.getId());
    }

    private void clôturerCandidatures(OffreEmploi offre) {
        List<Candidature> enCours = candidatureRepository.findByOffreIdAndStatutIn(
                offre.getId(), List.of(
                        StatutCandidature.RECU,
                        StatutCandidature.PRESELECTIONNE,
                        StatutCandidature.TEST_PLANIFIE,
                        StatutCandidature.TEST_PASSE,
                        StatutCandidature.ENTRETIEN_PLANIFIE,
                        StatutCandidature.ENTRETIEN_PASSE));
        enCours.forEach(candidature -> {
            StatutCandidature ancien = candidature.getStatut();
            candidature.setStatut(StatutCandidature.REJETE);
            candidatureRepository.save(candidature);
            historiqueRepository.save(CandidatureHistorique.builder()
                    .candidature(candidature).ancienStatut(ancien)
                    .nouveauStatut(StatutCandidature.REJETE)
                    .commentaire("Recrutement clôturé").date(LocalDateTime.now()).build());
            notificationService.notifyUserForOffer(
                    offre.getDemandeRecrutement().getDemandeur(),
                    "La candidature #" + candidature.getId()
                            + " a été clôturée avec l'offre #" + offre.getId() + ".",
                    offre.getId());
        });
        candidatureRepository.findByOffreIdAndStatut(
                offre.getId(), StatutCandidature.EN_ATTENTE,
                org.springframework.data.domain.Pageable.unpaged()).forEach(candidature -> {
                    notificationService.notifyUserForOffer(
                            offre.getDemandeRecrutement().getDemandeur(),
                            "La candidature #" + candidature.getId()
                                    + " reste en liste d'attente après clôture.",
                            offre.getId());
                });
    }

    private OffreEmploi getOffre(Long id) {
        return offreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Offre d'emploi", id));
    }

    private DemandeRecrutement getDemande(Integer id) {
        return demandeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demande de recrutement", id));
    }

    private void requireDemandeOuverte(OffreEmploi offre) {
        if (offre.getDemandeRecrutement().getStatut() != StatutDemande.RECRUTEMENT_OUVERT) {
            throw new IllegalStateException("L'offre ne peut être publiée que si la demande est RECRUTEMENT_OUVERT");
        }
    }

    private void requireRh(Utilisateur utilisateur) {
        if (!"RH".equals(utilisateur.getRole().getCode())) {
            throw new AccessDeniedException("Seule la RH peut gérer les offres d'emploi");
        }
    }
}
