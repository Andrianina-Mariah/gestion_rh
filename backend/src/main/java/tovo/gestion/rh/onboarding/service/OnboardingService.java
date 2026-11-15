package tovo.gestion.rh.onboarding.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import tovo.gestion.rh.common.exception.ResourceNotFoundException;
import tovo.gestion.rh.embauche.entity.Employe;
import tovo.gestion.rh.embauche.entity.StatutEmploye;
import tovo.gestion.rh.embauche.repository.AffectationRepository;
import tovo.gestion.rh.embauche.repository.EmployeRepository;
import tovo.gestion.rh.onboarding.dto.*;
import tovo.gestion.rh.onboarding.entity.*;
import tovo.gestion.rh.onboarding.event.OnboardingTermineEvent;
import tovo.gestion.rh.onboarding.repository.*;
import tovo.gestion.rh.utilisateur.entity.Utilisateur;
import tovo.gestion.rh.utilisateur.repository.UtilisateurRepository;
import tovo.gestion.rh.workflow.event.EmployeCreeEvent;
import tovo.gestion.rh.workflow.service.NotificationService;
import tovo.gestion.rh.embauche.entity.TypeDocument;
import tovo.gestion.rh.embauche.repository.TypeDocumentRepository;
import tovo.gestion.rh.audit.service.AuditService;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OnboardingService {
    private final OnboardingRepository onboardingRepository;
    private final OnboardingTacheRepository tacheRepository;
    private final OnboardingModeleRepository modeleRepository;
    private final OnboardingModeleTacheRepository modeleTacheRepository;
    private final EmployeRepository employeRepository;
    private final AffectationRepository affectationRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationService notificationService;
    private final MaterielAttribueRepository materielRepository;
    private final FormationAccueilRepository formationRepository;
    private final InscriptionPaieRepository paieRepository;
    private final DocumentEmployeRepository documentRepository;
    private final TypeDocumentRepository typeDocumentRepository;
    private final AuditService auditService;

    @Transactional
    public void creerDepuisEmploye(EmployeCreeEvent event) {
        if (onboardingRepository.findByEmployeId(event.employeId()).isPresent()) {
            return;
        }
        Employe employe = employeRepository.findById(event.employeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employé", event.employeId()));
        LocalDate debut = affectationRepository
                .findFirstByEmployeIdAndDateFinIsNullOrderByDateAffectationDesc(employe.getId())
                .map(a -> a.getDateAffectation())
                .orElse(employe.getDateEmbauche());
        Onboarding onboarding = onboardingRepository.save(Onboarding.builder()
                .employe(employe).dateDebut(debut).dateFinPrevue(debut.plusDays(30)).build());
        OnboardingModele modele = modelePour(employe);
        modeleTacheRepository.findByModeleIdOrderByIdAsc(modele.getId())
                .forEach(template -> {
                    tacheRepository.save(OnboardingTache.builder()
                            .onboarding(onboarding)
                            .libelle(template.getLibelle())
                            .type(template.getType())
                            .responsableRole(template.getResponsableRole())
                            .assignee(utilisateurRepository.findAll().stream()
                                    .filter(u -> u.getRole() != null
                                            && template.getResponsableRole().equals(u.getRole().getCode()))
                                    .findFirst().orElse(null))
                            .dateEcheance(debut.plusDays(template.getDelaiJours()))
                            .obligatoire(template.getObligatoire())
                            .build());
                });
        notificationService.notifyRole("RH", "Un nouvel onboarding a été créé pour l'employé "
                + employe.getMatricule() + ".", null);
    }

    @Transactional(readOnly = true)
    public List<OnboardingResponse> findAll(String statut, Utilisateur utilisateur) {
        requireAccess(utilisateur);
        List<Onboarding> onboardings = statut == null || statut.isBlank()
                ? onboardingRepository.findAllByOrderByDateDebutDesc()
                : onboardingRepository.findByStatutOrderByDateDebutDesc(
                        StatutOnboarding.valueOf(statut.toUpperCase()));
        return onboardings.stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public OnboardingResponse findById(Long id, Utilisateur utilisateur) {
        requireAccess(utilisateur);
        return response(getOnboarding(id));
    }

    @Transactional(readOnly = true)
    public OnboardingResponse findByEmploye(Long employeId, Utilisateur utilisateur) {
        requireAccess(utilisateur);
        return response(onboardingRepository.findByEmployeId(employeId)
                .orElseThrow(() -> new ResourceNotFoundException("Onboarding de l'employé", employeId)));
    }

    @Transactional
    public OnboardingTacheResponse demarrer(Long id, CommentaireTacheRequest request, Utilisateur utilisateur) {
        return transition(id, StatutOnboardingTache.EN_COURS, request.commentaire(), utilisateur);
    }

    @Transactional
    public OnboardingTacheResponse terminer(Long id, CommentaireTacheRequest request, Utilisateur utilisateur) {
        return transition(id, StatutOnboardingTache.FAIT, request.commentaire(), utilisateur);
    }

    @Transactional
    public OnboardingTacheResponse nonApplicable(Long id, CommentaireTacheRequest request, Utilisateur utilisateur) {
        return transition(id, StatutOnboardingTache.NON_APPLICABLE, request.commentaire(), utilisateur);
    }

    @Transactional
    public OnboardingTacheResponse ajouterTache(Long onboardingId, CreerOnboardingTacheRequest request,
                                                  Utilisateur utilisateur) {
        requireAccess(utilisateur);
        Onboarding onboarding = getOnboarding(onboardingId);
        verifierOuvert(onboarding);
        if (!request.dateEcheance().isAfter(onboarding.getDateDebut().minusDays(1))) {
            throw new IllegalArgumentException("La date d'échéance doit être postérieure ou égale au début de l'onboarding");
        }
        return OnboardingTacheResponse.from(tacheRepository.save(OnboardingTache.builder()
                .onboarding(onboarding).libelle(request.libelle())
                .type(TypeOnboardingTache.AUTRE)
                .responsableRole(request.responsableRole())
                .dateEcheance(request.dateEcheance())
                .obligatoire(request.obligatoire())
                .build()));
    }

    @Transactional
    public OnboardingTacheResponse reassigner(Long id, ReassignerTacheRequest request, Utilisateur utilisateur) {
        requireRole(utilisateur, "RH", "ADMIN");
        OnboardingTache tache = getTache(id);
        tache.setAssignee(utilisateurRepository.findById(request.assigneeId().longValue())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", request.assigneeId())));
        if (request.responsableRole() != null && !request.responsableRole().isBlank()) {
            tache.setResponsableRole(request.responsableRole().trim());
        }
        notificationService.notifyUser(tache.getAssignee(),
                "Une tâche d'onboarding vous a été assignée : " + tache.getLibelle(), null);
        return OnboardingTacheResponse.from(tacheRepository.save(tache));
    }

    @Transactional
    public OnboardingResponse cloturer(Long id, Utilisateur utilisateur) {
        requireAccess(utilisateur);
        Onboarding onboarding = getOnboarding(id);
        verifierOuvert(onboarding);
        List<OnboardingTache> taches = tacheRepository.findByOnboardingIdOrderByDateEcheanceAscIdAsc(id);
        if (taches.stream().anyMatch(t -> Boolean.TRUE.equals(t.getObligatoire())
                && t.getStatut() != StatutOnboardingTache.FAIT
                && t.getStatut() != StatutOnboardingTache.NON_APPLICABLE)) {
            throw new IllegalStateException("Toutes les tâches obligatoires doivent être FAIT ou NON_APPLICABLE");
        }
        onboarding.setStatut(StatutOnboarding.TERMINE);
        onboarding.setDateCloture(LocalDate.now());
        Employe employe = onboarding.getEmploye();
        employe.setStatut(StatutEmploye.ACTIF);
        employeRepository.save(employe);
        onboardingRepository.save(onboarding);
        eventPublisher.publishEvent(new OnboardingTermineEvent(onboarding.getId(), employe.getId()));
        notificationService.notifyRole("RH", "L'onboarding de l'employé "
                + employe.getMatricule() + " est terminé.", null);
        return response(onboarding);
    }

    @Transactional
    public OnboardingTacheResponse marquerTypeFait(Long employeId, TypeOnboardingTache type, String commentaire) {
        return onboardingRepository.findByEmployeId(employeId)
                .flatMap(onboarding -> tacheRepository
                        .findByOnboardingIdOrderByDateEcheanceAscIdAsc(onboarding.getId()).stream()
                        .filter(tache -> tache.getType() == type
                                && tache.getStatut() != StatutOnboardingTache.FAIT)
                        .findFirst())
                .map(tache -> {
                    tache.setStatut(StatutOnboardingTache.FAIT);
                    tache.setDateRealisation(LocalDate.now());
                    tache.setCommentaire(commentaire);
                    return OnboardingTacheResponse.from(tacheRepository.save(tache));
                })
                .orElse(null);
    }

    @Transactional
    public OnboardingTacheResponse badge(Long tacheId, BadgeRequest request, Utilisateur utilisateur) {
        requireAccess(utilisateur);
        OnboardingTache tache = getTache(tacheId);
        verifierResponsabilite(tache, utilisateur);
        verifierType(tache, TypeOnboardingTache.BADGE);
        tache.setBadgeNumero(request.numero());
        tache.setBadgeDateRemise(request.dateRemise());
        tache.setCommentaire(request.commentaire());
        tache.setStatut(StatutOnboardingTache.FAIT);
        tache.setDateRealisation(LocalDate.now());
        return OnboardingTacheResponse.from(tacheRepository.save(tache));
    }

    @Transactional
    public MaterielResponse materiel(Long tacheId, MaterielRequest request, Utilisateur utilisateur) {
        requireAccess(utilisateur);
        OnboardingTache tache = getTache(tacheId);
        verifierResponsabilite(tache, utilisateur);
        verifierType(tache, TypeOnboardingTache.MATERIEL);
        Employe employe = tache.getOnboarding().getEmploye();
        if (materielRepository.existsByNumeroSerie(request.numeroSerie())) {
            throw new IllegalArgumentException("Ce numéro de série est déjà attribué");
        }
        MaterielAttribue materiel = materielRepository.save(MaterielAttribue.builder()
                .employe(employe).onboardingTache(tache).typeMateriel(request.typeMateriel())
                .description(request.description()).numeroSerie(request.numeroSerie())
                .dateAttribution(LocalDate.now()).build());
        tache.setStatut(StatutOnboardingTache.FAIT);
        tache.setDateRealisation(LocalDate.now());
        tache.setCommentaire(request.commentaire());
        tacheRepository.save(tache);
        return MaterielResponse.from(materiel);
    }

    @Transactional
    public FormationAccueilResponse formation(Long tacheId, FormationAccueilRequest request, Utilisateur utilisateur) {
        requireAccess(utilisateur);
        OnboardingTache tache = getTache(tacheId);
        verifierResponsabilite(tache, utilisateur);
        verifierType(tache, TypeOnboardingTache.FORMATION);
        FormationAccueil formation = formationRepository.save(FormationAccueil.builder()
                .employe(tache.getOnboarding().getEmploye()).onboarding(tache.getOnboarding())
                .onboardingTache(tache).dateFormation(request.dateFormation())
                .dureeHeures(request.dureeHeures()).formateur(request.formateur()).build());
        tache.setStatut(StatutOnboardingTache.FAIT);
        tache.setDateRealisation(LocalDate.now());
        tache.setCommentaire(request.commentaire());
        tacheRepository.save(tache);
        return FormationAccueilResponse.from(formation);
    }

    @Transactional
    public InscriptionPaie paie(Long onboardingId, InscriptionPaieRequest request, Utilisateur utilisateur) {
        requireAccess(utilisateur);
        Onboarding onboarding = getOnboarding(onboardingId);
        Employe employe = onboarding.getEmploye();
        if (paieRepository.findByEmployeId(employe.getId()).isPresent()) {
            throw new IllegalStateException("L'employé est déjà inscrit à la paie");
        }
        InscriptionPaie inscription = paieRepository.save(InscriptionPaie.builder()
                .employe(employe).dateInscription(LocalDate.now()).statut("INSCRIT")
                .referencePaie(request.referencePaie()).modePaiement(request.modePaiement())
                .numeroCompte(request.numeroCompte()).build());
        marquerTypeFait(employe.getId(), TypeOnboardingTache.PAIE, request.commentaire());
        return inscription;
    }

    @Transactional
    public DocumentResponse document(Long employeId, Long tacheId, String type, MultipartFile file,
                                     LocalDate dateEmission, LocalDate dateExpiration,
                                     Utilisateur utilisateur) {
        requireDocumentAccess(employeId, utilisateur);
        if (file == null || file.isEmpty() || file.getSize() > 5 * 1024 * 1024
                || !"application/pdf".equalsIgnoreCase(file.getContentType())
                || !isPdf(file)) {
            throw new IllegalArgumentException("Le document doit être un PDF de 5 Mo maximum");
        }
        Employe employe = employeRepository.findById(employeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", employeId));
        TypeDocument typeDocument = typeDocumentRepository.findByCode(type.trim().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Type de document inconnu"));
        OnboardingTache tache = tacheId == null ? null : getTache(tacheId);
        if (tache != null && !tache.getOnboarding().getEmploye().getId().equals(employeId)) {
            throw new AccessDeniedException("La tâche n'appartient pas à cet employé");
        }
        try {
            Path dir = Paths.get("uploads", "onboarding").toAbsolutePath().normalize();
            Files.createDirectories(dir);
            String filename = UUID.randomUUID() + ".pdf";
            Path target = dir.resolve(filename).normalize();
            if (!target.startsWith(dir)) {
                throw new IllegalArgumentException("Nom de fichier invalide");
            }
            Files.copy(file.getInputStream(), target);
            int version = documentRepository.findByEmployeIdAndTypeOrderByVersionDesc(employeId, typeDocument.getCode())
                    .stream().findFirst().map(d -> d.getVersion() + 1).orElse(1);
            DocumentEmploye document = documentRepository.save(DocumentEmploye.builder()
                    .employe(employe).onboardingTache(tache).type(type)
                    .typeDocument(typeDocument).nomFichier(file.getOriginalFilename())
                    .url("/uploads/onboarding/" + filename).taille(file.getSize())
                    .dateDepot(LocalDateTime.now()).dateEmission(dateEmission).dateExpiration(dateExpiration)
                    .confidentialite(typeDocument.getConfidentialite()).version(version)
                    .deposePar(utilisateur).build());
            auditService.record(utilisateur, "CREATION", "Document", document.getId(),
                    null, "DEPOT:" + document.getType(), null);
            return DocumentResponse.from(document);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible d'enregistrer le document", e);
        }
    }

    @Transactional
    public DocumentEmploye getDocument(Long id, Utilisateur utilisateur) {
        DocumentEmploye document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", id));
        requireDocumentAccess(document.getEmploye().getId(), utilisateur);
        if (List.of("SENSIBLE", "TRES_SENSIBLE").contains(document.getConfidentialite())) {
            requireRole(utilisateur, "RH", "ADMIN", "AUDITEUR", "EMPLOYE");
            auditService.record(utilisateur, "LECTURE", "Document", id, null, "TELECHARGEMENT_CONFIDENTIEL", null);
        }
        return document;
    }

    @Transactional
    public DocumentResponse validateDocument(Long id, Utilisateur utilisateur) {
        requireRole(utilisateur, "RH", "ADMIN");
        DocumentEmploye document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", id));
        document.setStatut("VALIDE");
        document.setValidePar(utilisateur);
        document.setDateValidation(LocalDateTime.now());
        document.setMotifRejet(null);
        auditService.record(utilisateur, "MODIFICATION", "Document", id, "DEPOSE", "VALIDE", null);
        return DocumentResponse.from(documentRepository.save(document));
    }

    @Transactional
    public DocumentResponse rejectDocument(Long id, String motif, Utilisateur utilisateur) {
        requireRole(utilisateur, "RH", "ADMIN");
        if (motif == null || motif.isBlank()) {
            throw new IllegalArgumentException("Le motif de rejet est obligatoire");
        }
        DocumentEmploye document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", id));
        document.setStatut("REJETE");
        document.setValidePar(utilisateur);
        document.setDateValidation(LocalDateTime.now());
        document.setMotifRejet(motif.trim());
        auditService.record(utilisateur, "MODIFICATION", "Document", id, "DEPOSE", "REJETE", null);
        notificationService.notifyUser(document.getDeposePar(),
                "Document rejeté : " + document.getNomFichier() + " (" + motif.trim() + ")", null);
        return DocumentResponse.from(documentRepository.save(document));
    }

    @Transactional(readOnly = true)
    public List<DocumentManquantResponse> missingDocuments(Long employeId, Utilisateur utilisateur) {
        requireDocumentAccess(employeId, utilisateur);
        return missingForEmployee(employeId);
    }

    @Transactional(readOnly = true)
    public List<DocumentManquantResponse> globalMissingDocuments(Utilisateur utilisateur) {
        requireRole(utilisateur, "RH", "ADMIN");
        return employeRepository.findAll().stream()
                .flatMap(e -> missingForEmployee(e.getId()).stream()).toList();
    }

    @Transactional(readOnly = true)
    public List<DocumentAlerteResponse> expiringDocuments(int jours, Utilisateur utilisateur) {
        requireRole(utilisateur, "RH", "ADMIN");
        if (jours < 0 || jours > 3650) {
            throw new IllegalArgumentException("Le nombre de jours doit être compris entre 0 et 3650");
        }
        LocalDate today = LocalDate.now();
        return documentRepository.findByDateExpirationBetweenAndStatut(today, today.plusDays(jours), "VALIDE")
                .stream().map(d -> new DocumentAlerteResponse(d.getId(), d.getEmploye().getId(),
                        d.getType(), d.getStatut(), d.getDateExpiration(), "EXPIRE_DANS_" + jours + "_JOURS")).toList();
    }

    @Scheduled(cron = "${app.documents.expiry-cron:0 15 8 * * *}")
    @Transactional
    public void expireDocuments() {
        LocalDate today = LocalDate.now();
        documentRepository.findByDateExpirationBeforeAndStatut(today, "VALIDE").forEach(document -> {
            document.setStatut("EXPIRE");
            documentRepository.save(document);
            notificationService.notifyRole("RH", "Document expiré pour l'employé "
                    + document.getEmploye().getMatricule() + " : " + document.getType(), null);
        });
        LocalDate alertDate = today.plusDays(30);
        documentRepository.findByDateExpirationBetweenAndStatut(today, alertDate, "VALIDE")
                .forEach(document -> notificationService.notifyRole("RH",
                        "Document à renouveler avant le " + document.getDateExpiration()
                                + " pour l'employé " + document.getEmploye().getMatricule(), null));
        employeRepository.findAll().forEach(employe -> missingForEmployee(employe.getId()).forEach(missing ->
                notificationService.notifyRole("RH", "Document manquant pour l'employé "
                        + employe.getMatricule() + " : " + missing.libelle(), null)));
    }

    private List<DocumentManquantResponse> missingForEmployee(Long employeId) {
        return typeDocumentRepository.findByObligatoireTrue().stream()
                .filter(type -> documentRepository.findByEmployeIdAndTypeOrderByVersionDesc(employeId, type.getCode())
                        .stream().noneMatch(d -> "VALIDE".equals(d.getStatut())
                                && (d.getDateExpiration() == null || !d.getDateExpiration().isBefore(LocalDate.now()))))
                .map(type -> new DocumentManquantResponse(employeId, type.getCode(), type.getLibelle(),
                        "ABSENT_OU_EXPIRE")).toList();
    }

    private boolean isPdf(MultipartFile file) {
        try {
            byte[] header = file.getInputStream().readNBytes(5);
            return header.length == 5 && "%PDF-".equals(new String(header, java.nio.charset.StandardCharsets.US_ASCII));
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de vérifier le fichier", e);
        }
    }

    private void requireDocumentAccess(Long employeId, Utilisateur utilisateur) {
        String role = utilisateur.getRole() == null ? null : utilisateur.getRole().getCode();
        if (List.of("RH", "ADMIN", "IT").contains(role)) return;
        if ("MANAGER".equals(role) && isTeamMember(employeId, utilisateur)) return;
        if ("EMPLOYE".equals(role) && employeRepository.findById(employeId)
                .map(e -> e.getCandidat() != null && utilisateur.getEmail().equalsIgnoreCase(e.getCandidat().getEmail()))
                .orElse(false)) return;
        throw new AccessDeniedException("Accès au document refusé");
    }

    private boolean isTeamMember(Long employeId, Utilisateur manager) {
        Long managerId = employeRepository.findAll().stream()
                .filter(e -> e.getCandidat() != null
                        && manager.getEmail().equalsIgnoreCase(e.getCandidat().getEmail()))
                .map(Employe::getId).findFirst().orElse(null);
        return managerId != null && affectationRepository
                .findFirstByEmployeIdAndDateFinIsNullOrderByDateAffectationDesc(employeId)
                .map(a -> a.getManager() != null && managerId.equals(a.getManager().getId()))
                .orElse(false);
    }

    private void requireRole(Utilisateur utilisateur, String... roles) {
        String role = utilisateur.getRole() == null ? null : utilisateur.getRole().getCode();
        if (role == null || java.util.Arrays.stream(roles).noneMatch(role::equals)) {
            throw new AccessDeniedException("Rôle insuffisant");
        }
    }

    private void verifierType(OnboardingTache tache, TypeOnboardingTache type) {
        if (tache.getType() != type) {
            throw new IllegalArgumentException("Cette tâche n'est pas de type " + type);
        }
        verifierOuvert(tache.getOnboarding());
    }

    @Scheduled(cron = "${app.onboarding.deadline-cron:0 0 8 * * *}")
    @Transactional
    public void notifierEcheances() {
        LocalDate limite = LocalDate.now().plusDays(3);
        tacheRepository.findAll().stream()
                .filter(t -> t.getStatut() != StatutOnboardingTache.FAIT
                        && t.getStatut() != StatutOnboardingTache.NON_APPLICABLE
                        && !t.getDateEcheance().isAfter(limite)
                        && t.getAssignee() != null)
                .forEach(t -> notificationService.notifyUser(t.getAssignee(),
                        "Échéance d'onboarding " + (t.getDateEcheance().isBefore(LocalDate.now())
                                ? "dépassée" : "proche") + " : " + t.getLibelle(), null));
    }

    private OnboardingTacheResponse transition(Long id, StatutOnboardingTache statut, String commentaire,
                                                Utilisateur utilisateur) {
        requireAccess(utilisateur);
        OnboardingTache tache = getTache(id);
        verifierResponsabilite(tache, utilisateur);
        verifierOuvert(tache.getOnboarding());
        tache.setStatut(statut);
        tache.setCommentaire(commentaire.trim());
        tache.setDateRealisation(statut == StatutOnboardingTache.FAIT ? LocalDate.now() : null);
        Onboarding onboarding = tache.getOnboarding();
        if (statut == StatutOnboardingTache.EN_COURS && onboarding.getStatut() == StatutOnboarding.A_FAIRE) {
            onboarding.setStatut(StatutOnboarding.EN_COURS);
            onboardingRepository.save(onboarding);
        }
        return OnboardingTacheResponse.from(tacheRepository.save(tache));
    }

    private OnboardingModele modelePour(Employe employe) {
        var affectation = affectationRepository
                .findFirstByEmployeIdAndDateFinIsNullOrderByDateAffectationDesc(employe.getId()).orElse(null);
        if (affectation != null && affectation.getPoste() != null) {
            var modele = modeleRepository.findFirstByPosteIdAndActifTrue(affectation.getPoste().getId());
            if (modele.isPresent()) return modele.get();
            if (affectation.getPoste().getDepartement() != null) {
                modele = modeleRepository.findFirstByDepartementIdAndActifTrue(
                        affectation.getPoste().getDepartement().getId());
                if (modele.isPresent()) return modele.get();
            }
        }
        return modeleRepository.findFirstByPosteIsNullAndDepartementIsNullAndActifTrue()
                .orElseThrow(() -> new IllegalStateException("Aucun modèle d'onboarding actif"));
    }

    private OnboardingResponse response(Onboarding onboarding) {
        return OnboardingResponse.from(onboarding,
                tacheRepository.findByOnboardingIdOrderByDateEcheanceAscIdAsc(onboarding.getId())
                        .stream().map(OnboardingTacheResponse::from).toList());
    }

    private Onboarding getOnboarding(Long id) {
        return onboardingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Onboarding", id));
    }

    private OnboardingTache getTache(Long id) {
        return tacheRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tâche onboarding", id));
    }

    private void verifierOuvert(Onboarding onboarding) {
        if (onboarding.getStatut() == StatutOnboarding.TERMINE
                || onboarding.getStatut() == StatutOnboarding.ANNULE) {
            throw new IllegalStateException("L'onboarding est clôturé");
        }
    }

    private void requireAccess(Utilisateur utilisateur) {
        String role = utilisateur.getRole() == null ? null : utilisateur.getRole().getCode();
        if (!List.of("RH", "ADMIN", "IT", "MANAGER", "SERVICES_GENERAUX").contains(role)) {
            throw new AccessDeniedException("Accès réservé aux acteurs de l'onboarding");
        }
    }

    private void verifierResponsabilite(OnboardingTache tache, Utilisateur utilisateur) {
        String role = utilisateur.getRole() == null ? null : utilisateur.getRole().getCode();
        if ("RH".equals(role) || "ADMIN".equals(role)) return;
        boolean assignedToUser = tache.getAssignee() != null
                && tache.getAssignee().getId().equals(utilisateur.getId());
        boolean unassignedRoleTask = tache.getAssignee() == null
                && role != null && role.equals(tache.getResponsableRole());
        if (!assignedToUser && !unassignedRoleTask) {
            throw new AccessDeniedException("Cette tâche est réservée à " + tache.getResponsableRole()
                    + " ou à son responsable assigné");
        }
    }
}
