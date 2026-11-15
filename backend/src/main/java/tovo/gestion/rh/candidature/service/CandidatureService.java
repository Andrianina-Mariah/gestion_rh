package tovo.gestion.rh.candidature.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.AccessDeniedException;
import tovo.gestion.rh.candidature.dto.*;
import tovo.gestion.rh.candidature.entity.*;
import tovo.gestion.rh.candidature.repository.*;
import tovo.gestion.rh.common.exception.ResourceNotFoundException;
import tovo.gestion.rh.offreemploi.entity.OffreEmploi;
import tovo.gestion.rh.offreemploi.entity.StatutOffre;
import tovo.gestion.rh.offreemploi.repository.OffreEmploiRepository;
import tovo.gestion.rh.workflow.service.NotificationService;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CandidatureService {
    private final OffreEmploiRepository offreRepository;
    private final CandidatRepository candidatRepository;
    private final CvCandidatureRepository cvRepository;
    private final CandidatureRepository candidatureRepository;
    private final CandidatureHistoriqueRepository historiqueRepository;
    private final CandidatureFileStorage fileStorage;
    private final NotificationService notificationService;

    @Transactional
    public CandidatureResponse postuler(Long offreId, CandidatureRequest request,
                                        MultipartFile cvFile, MultipartFile lettreFile) {
        OffreEmploi offre = getOffre(offreId);
        if (offre.getStatut() != StatutOffre.PUBLIEE) {
            throw new IllegalStateException("Les candidatures ne sont acceptées que pour une offre PUBLIEE");
        }

        Candidat candidat = candidatRepository.findByEmailIgnoreCase(request.email())
                .orElseGet(Candidat::new);
        candidat.setNom(request.nom());
        candidat.setPrenom(request.prenom());
        candidat.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
        candidat.setTelephone(request.telephone());
        candidat.setAdresse(request.adresse());
        candidat = candidatRepository.save(candidat);

        if (candidatureRepository.existsByOffreIdAndCandidatId(offreId, candidat.getId())) {
            throw new IllegalStateException("Ce candidat a déjà postulé à cette offre");
        }

        CvCandidature cv = CvCandidature.builder()
                .candidat(candidat)
                .titreProfil(request.titreProfil())
                .resume(request.resume())
                .competences(request.competences())
                .langues(request.langues())
                .build();
        if (request.experiences() != null) {
            for (ExperienceRequest item : request.experiences()) {
                cv.getExperiences().add(ExperienceProfessionnelle.builder()
                        .cv(cv).poste(item.poste()).entreprise(item.entreprise())
                        .dateDebut(item.dateDebut()).dateFin(item.dateFin())
                        .enPoste(item.enPoste()).description(item.description()).build());
            }
        }
        if (request.formations() != null) {
            for (FormationRequest item : request.formations()) {
                cv.getFormations().add(FormationCandidat.builder()
                        .cv(cv).diplome(item.diplome()).etablissement(item.etablissement())
                        .anneeObtention(item.anneeObtention()).description(item.description()).build());
            }
        }
        cv = cvRepository.save(cv);

        Candidature candidature = Candidature.builder()
                .offre(offre).candidat(candidat).cv(cv)
                .cvUrl(fileStorage.store(cvFile, true))
                .lettreMotivationUrl(fileStorage.store(lettreFile, false))
                .cvSnapshot(snapshot(cv))
                .statut(StatutCandidature.RECU)
                .build();
        candidature = candidatureRepository.save(candidature);
        CandidatureHistorique historique = CandidatureHistorique.builder()
                .candidature(candidature).nouveauStatut(StatutCandidature.RECU)
                .commentaire("Candidature déposée").date(java.time.LocalDateTime.now()).build();
        historiqueRepository.save(historique);
        notificationService.notifyRole("RH",
                "Nouvelle candidature reçue pour l'offre #" + offreId + ".",
                offre.getDemandeRecrutement().getId());
        return CandidatureResponse.from(candidature, List.of(HistoriqueResponse.from(historique)));
    }

    @Transactional(readOnly = true)
    public Page<CandidatureResponse> findByOffre(Long offreId, StatutCandidature statut,
                                                  Pageable pageable, tovo.gestion.rh.utilisateur.entity.Utilisateur utilisateur) {
        OffreEmploi offre = getOffre(offreId);
        requireOfferAccess(offre, utilisateur);
        Page<Candidature> page = statut == null
                ? candidatureRepository.findByOffreId(offreId, pageable)
                : candidatureRepository.findByOffreIdAndStatut(offreId, statut, pageable);
        return page.map(c -> CandidatureResponse.from(c, historique(c.getId())));
    }

    @Transactional(readOnly = true)
    public CandidatureResponse findById(Long id, tovo.gestion.rh.utilisateur.entity.Utilisateur utilisateur) {
        Candidature candidature = candidatureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidature", id));
        requireOfferAccess(candidature.getOffre(), utilisateur);
        return CandidatureResponse.from(candidature, historique(id));
    }

    @Transactional(readOnly = true)
    public org.springframework.core.io.Resource loadFile(String filename) {
        return fileStorage.load(filename);
    }

    private List<HistoriqueResponse> historique(Long candidatureId) {
        return historiqueRepository.findByCandidatureIdOrderByDateAsc(candidatureId).stream()
                .map(HistoriqueResponse::from).toList();
    }

    private Map<String, Object> snapshot(CvCandidature cv) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("titreProfil", cv.getTitreProfil());
        result.put("resume", cv.getResume());
        result.put("competences", cv.getCompetences());
        result.put("langues", cv.getLangues());
        result.put("experiences", cv.getExperiences().stream().map(e -> Map.of(
                "poste", e.getPoste(), "entreprise", e.getEntreprise(),
                "dateDebut", Objects.toString(e.getDateDebut(), ""),
                "dateFin", Objects.toString(e.getDateFin(), ""),
                "enPoste", e.isEnPoste(), "description", Objects.toString(e.getDescription(), "")
        )).toList());
        result.put("formations", cv.getFormations().stream().map(f -> Map.of(
                "diplome", f.getDiplome(), "etablissement", f.getEtablissement(),
                "anneeObtention", Objects.toString(f.getAnneeObtention(), ""),
                "description", Objects.toString(f.getDescription(), "")
        )).toList());
        return result;
    }

    private OffreEmploi getOffre(Long id) {
        return offreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Offre d'emploi", id));
    }

    private void requireOfferAccess(OffreEmploi offre, tovo.gestion.rh.utilisateur.entity.Utilisateur utilisateur) {
        String role = utilisateur.getRole().getCode();
        if ("RH".equals(role) || "ADMIN".equals(role)) {
            return;
        }
        if ("MANAGER".equals(role)
                && offre.getDemandeRecrutement().getDemandeur().getId().equals(utilisateur.getId())) {
            return;
        }
        throw new AccessDeniedException("Vous ne pouvez pas consulter cette offre");
    }
}
