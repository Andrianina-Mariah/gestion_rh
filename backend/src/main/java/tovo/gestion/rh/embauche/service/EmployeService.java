package tovo.gestion.rh.embauche.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import tovo.gestion.rh.common.exception.ResourceNotFoundException;
import tovo.gestion.rh.embauche.dto.*;
import tovo.gestion.rh.embauche.entity.Affectation;
import tovo.gestion.rh.embauche.entity.Employe;
import tovo.gestion.rh.embauche.entity.StatutEmploye;
import tovo.gestion.rh.embauche.repository.AffectationRepository;
import tovo.gestion.rh.embauche.repository.ContratRepository;
import tovo.gestion.rh.embauche.repository.EmployeRepository;
import tovo.gestion.rh.embauche.repository.*;
import tovo.gestion.rh.embauche.entity.*;
import tovo.gestion.rh.onboarding.dto.DocumentResponse;
import tovo.gestion.rh.onboarding.repository.DocumentEmployeRepository;
import tovo.gestion.rh.utilisateur.entity.Utilisateur;
import tovo.gestion.rh.poste.repository.PosteRepository;
import tovo.gestion.rh.departement.repository.DepartementRepository;
import tovo.gestion.rh.embauche.event.*;
import tovo.gestion.rh.audit.service.AuditService;
import tovo.gestion.rh.workflow.service.NotificationService;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeService {

    private final EmployeRepository employeRepository;
    private final AffectationRepository affectationRepository;
    private final ContratRepository contratRepository;
    private final EmployeAdresseRepository adresseRepository;
    private final EmployeContactRepository contactRepository;
    private final ContactUrgenceRepository urgenceRepository;
    private final AyantDroitRepository ayantDroitRepository;
    private final HistoriqueSalaireRepository salaireRepository;
    private final DocumentEmployeRepository documentRepository;
    private final DemandeModificationRepository demandeRepository;
    private final PosteRepository posteRepository;
    private final DepartementRepository departementRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditService auditService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<EmployeResponse> findAll(String statut, Integer departementId, String poste,
                                         String recherche, int page, int size, String sort, Utilisateur utilisateur) {
        ensureAccess(utilisateur);
        List<Employe> employes = employeRepository.findAllByOrderByIdDesc();
        if ("MANAGER".equals(utilisateur.getRole().getCode())) {
            employes = employes.stream().filter(e -> isTeamMember(e.getId(), utilisateur)).toList();
        } else if ("EMPLOYE".equals(utilisateur.getRole().getCode())) {
            Long ownId = employeeForUser(utilisateur).getId();
            employes = employes.stream().filter(e -> e.getId().equals(ownId)).toList();
        }

        if (statut != null && !statut.isBlank()) {
            employes = employes.stream()
                    .filter(e -> e.getStatut() == StatutEmploye.valueOf(statut.trim().toUpperCase()))
                    .toList();
        }

        if (departementId != null) {
            employes = employes.stream()
                    .filter(e -> affectationRepository.findFirstByEmployeIdAndDateFinIsNullOrderByDateAffectationDesc(e.getId())
                            .map(a -> a.getDepartement() != null && a.getDepartement().getId().equals(departementId))
                            .orElse(false))
                    .toList();
        }
        if (poste != null && !poste.isBlank()) {
            String value = poste.trim().toLowerCase();
            employes = employes.stream().filter(e -> affectationRepository
                    .findFirstByEmployeIdAndDateFinIsNullOrderByDateAffectationDesc(e.getId())
                    .map(a -> a.getPoste() != null && a.getPoste().getTitre().toLowerCase().contains(value))
                    .orElse(false)).toList();
        }

        if (recherche != null && !recherche.isBlank()) {
            String query = recherche.trim().toLowerCase();
            employes = employes.stream()
                    .filter(e -> {
                        var candidat = e.getCandidat();
                        if (candidat == null) return false;
                        return (candidat.getNom() != null && candidat.getNom().toLowerCase().contains(query))
                                || (candidat.getPrenom() != null && candidat.getPrenom().toLowerCase().contains(query))
                                || (candidat.getEmail() != null && candidat.getEmail().toLowerCase().contains(query))
                                || (e.getMatricule() != null && e.getMatricule().toLowerCase().contains(query));
                    })
                    .toList();
        }

        var sorted = employes.stream();
        if ("nom".equalsIgnoreCase(sort)) {
            sorted = sorted.sorted((a, b) -> a.getCandidat().getNom().compareToIgnoreCase(b.getCandidat().getNom()));
        } else if ("dateEmbauche".equalsIgnoreCase(sort)) {
            sorted = sorted.sorted((a, b) -> b.getDateEmbauche().compareTo(a.getDateEmbauche()));
        }
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 200);
        return sorted
                .skip((long) safePage * safeSize).limit(safeSize)
                .map(e -> toResponseFor(e, utilisateur))
                .toList();
    }

    @Transactional(readOnly = true)
    public EmployeResponse findById(Long id, Utilisateur utilisateur) {
        ensureEmployeeAccess(id, utilisateur);
        return toResponseFor(getEmploye(id), utilisateur);
    }

    @Transactional
    public DossierEmployeResponse dossier(Long id, Utilisateur utilisateur) {
        ensureEmployeeAccess(id, utilisateur);
        Employe e = getEmploye(id);
        auditService.record(utilisateur, "LECTURE", "Employe", id, null, "DONNEES_SENSIBLES", null);
        List<SalaireResponse> salaires = salaireRepository.findByEmployeIdOrderByDateDebutDesc(id).stream().map(SalaireResponse::from).toList();
        LocalDate premiereDateContrat = contratRepository.findByEmployeIdOrderByDateDebutAsc(id).stream()
                .map(Contrat::getDateDebut).min(LocalDate::compareTo).orElse(e.getDateEmbauche());
        java.time.Period anciennete = java.time.Period.between(premiereDateContrat, LocalDate.now());
        String ancienneteLabel = anciennete.getYears() + " an(s), " + anciennete.getMonths() + " mois";
        boolean sensitive = canReadSensitive(utilisateur);
        return new DossierEmployeResponse(toResponseFor(e, utilisateur), ancienneteLabel,
                addressesForDossier(e),
                contactRepository.findByEmployeIdOrderByDateDebutDesc(id).stream().map(ContactResponse::from).toList(),
                urgenceRepository.findByEmployeIdOrderByPrincipalDescIdAsc(id).stream().map(ContactUrgenceResponse::from).toList(),
                ayantDroitRepository.findByEmployeIdOrderByDateNaissanceAsc(id).stream().map(AyantDroitResponse::from).toList(),
                contratRepository.findByEmployeIdOrderByDateDebutAsc(id).stream().map(ContratResponse::from).toList(),
                affectationRepository.findByEmployeIdOrderByDateAffectationDesc(id).stream().map(AffectationResponse::from).toList(),
                documentRepository.findByEmployeIdOrderByDateDepotDesc(id).stream()
                        .filter(d -> sensitive || "PUBLIC".equals(d.getConfidentialite())
                                || "INTERNE".equals(d.getConfidentialite()))
                        .map(DocumentResponse::from).toList(),
                sensitive ? salaires : List.of(), sensitive && !salaires.isEmpty()
                        ? salaires.stream().filter(s -> s.dateFin() == null).findFirst().orElse(null) : null,
                demandeRepository.findByEmployeIdOrderByDateDemandeDesc(id).stream().map(DemandeModificationResponse::from).toList());
    }

    @Transactional
    public DossierEmployeResponse dossierPersonnel(Utilisateur utilisateur) {
        Employe e = employeeForUser(utilisateur);
        DossierEmployeResponse dossier = dossier(e.getId(), utilisateur);
        EmployeResponse identity = dossier.identite();
        EmployeResponse sanitized = new EmployeResponse(
                identity.id(), identity.candidatId(), identity.matricule(), identity.nom(), identity.prenom(),
                identity.email(), identity.telephone(), identity.statut(), identity.dateEmbauche(),
                null, null, null, null, null, null, null, null, null, null,
                identity.poste(), identity.departement(), identity.affectationCourante(), null);
        return new DossierEmployeResponse(sanitized, dossier.anciennete(), dossier.adresses(), dossier.contacts(),
                dossier.contactsUrgence(), dossier.ayantsDroit(), List.of(), dossier.affectations(),
                dossier.documents(), List.of(), null, dossier.demandesModification());
    }

    @Transactional(readOnly = true)
    public List<CarriereEventResponse> carriere(Long id, Utilisateur utilisateur) {
        ensureEmployeeAccess(id, utilisateur);
        List<CarriereEventResponse> events = new java.util.ArrayList<>();
        contratRepository.findByEmployeIdOrderByDateDebutAsc(id).forEach(c ->
                events.add(new CarriereEventResponse("CONTRAT", c.getDateDebut(), c.getDateFin(),
                        c.getTypeContrat().name(), "Salaire " + c.getSalaire())));
        affectationRepository.findByEmployeIdOrderByDateAffectationDesc(id).forEach(a ->
                events.add(new CarriereEventResponse("AFFECTATION", a.getDateAffectation(), a.getDateFin(),
                        a.getPoste().getTitre(), a.getDepartement().getNom())));
        salaireRepository.findByEmployeIdOrderByDateDebutDesc(id).forEach(s ->
                events.add(new CarriereEventResponse("SALAIRE", s.getDateDebut(), s.getDateFin(),
                        s.getMontant() + " " + s.getDevise(), s.getMotif())));
        return events.stream().sorted(java.util.Comparator.comparing(CarriereEventResponse::dateDebut)).toList();
    }

    @Transactional
    public AffectationCareerResponse changeAffectation(Long id, AffectationRequest request, Utilisateur utilisateur) {
        requireRole(utilisateur, "RH", "ADMIN");
        Employe employe = getEmploye(id);
        Affectation current = affectationRepository
                .findFirstByEmployeIdAndDateFinIsNullOrderByDateAffectationDesc(id).orElse(null);
        if (current != null) {
            if (!request.dateAffectation().isAfter(current.getDateAffectation())) {
                throw new IllegalArgumentException("La nouvelle affectation doit commencer après l'actuelle");
            }
            current.setDateFin(request.dateAffectation().minusDays(1));
            affectationRepository.save(current);
        }
        Employe manager = request.managerId() == null ? null : getEmploye(request.managerId());
        if (manager != null && manager.getId().equals(id)) {
            throw new IllegalArgumentException("Un employé ne peut pas être son propre manager");
        }
        if (manager != null && reportsTo(manager, id)) {
            throw new IllegalArgumentException("Cette affectation créerait un cycle hiérarchique");
        }
        Affectation next = Affectation.builder().employe(employe)
                .poste(posteRepository.findById(request.posteId()).orElseThrow(() -> new ResourceNotFoundException("Poste", request.posteId())))
                .departement(departementRepository.findById(request.departementId()).orElseThrow(() -> new ResourceNotFoundException("Département", request.departementId())))
                .manager(manager).dateAffectation(request.dateAffectation()).build();
        next = affectationRepository.save(next);
        eventPublisher.publishEvent(new AffectationChangeeEvent(id, next.getId()));
        auditService.record(utilisateur, "MODIFICATION", "Affectation", next.getId(),
                "AFFECTATION_COURANTE_CLOTUREE", "NOUVELLE_AFFECTATION", null);
        return new AffectationCareerResponse(AffectationResponse.from(next), manager == null ? null : manager.getId());
    }

    @Transactional
    public SalaireResponse changeSalaire(Long id, SalaireRequest request, Utilisateur utilisateur) {
        requireRole(utilisateur, "RH", "ADMIN");
        Employe employe = getEmploye(id);
        salaireRepository.findByEmployeIdOrderByDateDebutDesc(id).stream()
                .filter(s -> s.getDateFin() == null).findFirst().ifPresent(previous -> {
                    if (!request.dateDebut().isAfter(previous.getDateDebut())) {
                        throw new IllegalArgumentException("Le nouveau salaire doit commencer après l'actuel");
                    }
                    previous.setDateFin(request.dateDebut().minusDays(1));
                    salaireRepository.save(previous);
                });
        Contrat contrat = request.contratId() == null ? null : contratRepository.findById(request.contratId())
                .orElseThrow(() -> new ResourceNotFoundException("Contrat", request.contratId()));
        HistoriqueSalaire salary = salaireRepository.save(HistoriqueSalaire.builder().employe(employe)
                .montant(request.montant()).devise(request.devise() == null ? "MGA" : request.devise())
                .dateDebut(request.dateDebut()).motif(request.motif()).contrat(contrat).build());
        eventPublisher.publishEvent(new SalaireChangeEvent(id, salary.getId()));
        auditService.record(utilisateur, "MODIFICATION", "HistoriqueSalaire", salary.getId(),
                "SALAIRE_PRECEDENT_CLOTURE", "NOUVEAU_SALAIRE", null);
        return SalaireResponse.from(salary);
    }

    @Transactional
    public EmployeResponse sortie(Long id, SortieRequest request, Utilisateur utilisateur) {
        requireRole(utilisateur, "RH", "ADMIN");
        if (!java.util.Set.of("DEMISSION", "LICENCIEMENT", "FIN_CONTRAT", "RETRAITE").contains(request.motif())) {
            throw new IllegalArgumentException("Motif de sortie invalide");
        }
        Employe employe = getEmploye(id);
        affectationRepository.findFirstByEmployeIdAndDateFinIsNullOrderByDateAffectationDesc(id).ifPresent(a -> {
            a.setDateFin(request.dateSortie());
            affectationRepository.save(a);
        });
        employe.setStatut(StatutEmploye.ARCHIVE);
        employe.setDateSortie(request.dateSortie());
        employe.setMotifSortie(request.motif());
        employe = employeRepository.save(employe);
        eventPublisher.publishEvent(new EmployeSortiEvent(id, request.motif()));
        auditService.record(utilisateur, "MODIFICATION", "Employe", id,
                "ACTIF", employe.getStatut().name(), null);
        return toResponseFor(employe, utilisateur);
    }

    private boolean reportsTo(Employe candidate, Long employeeId) {
        Employe current = candidate;
        java.util.Set<Long> visited = new java.util.HashSet<>();
        while (current != null && visited.add(current.getId())) {
            Affectation assignment = affectationRepository
                    .findFirstByEmployeIdAndDateFinIsNullOrderByDateAffectationDesc(current.getId()).orElse(null);
            current = assignment == null ? null : assignment.getManager();
            if (current != null && current.getId().equals(employeeId)) return true;
        }
        return false;
    }

    @Transactional
    public EmployeResponse updateIdentite(Long id, IdentiteRequest request, Utilisateur utilisateur) {
        requireRole(utilisateur, "RH", "ADMIN");
        validateBirthDate(request.dateNaissance());
        Employe e = getEmploye(id);
        e.setDateNaissance(request.dateNaissance()); e.setLieuNaissance(request.lieuNaissance());
        e.setSexe(request.sexe()); e.setNationalite(request.nationalite()); e.setCin(request.cin());
        e.setNumeroCnaps(request.numeroCnaps()); e.setSituationFamiliale(request.situationFamiliale());
        e.setNombreEnfants(request.nombreEnfants());
        return toResponse(employeRepository.save(e));
    }

    @Transactional
    public AdresseResponse addAddress(Long id, AdresseRequest request, Utilisateur utilisateur) {
        ensureEditable(id, utilisateur);
        validateType(request.type(), "PRINCIPALE", "SECONDAIRE");
        adresseRepository.findByEmployeIdOrderByDateDebutDesc(id).stream()
                .filter(a -> request.type().equals(a.getType()) && a.getDateFin() == null)
                .forEach(previous -> {
                    if (!request.dateDebut().isAfter(previous.getDateDebut())) {
                        throw new IllegalArgumentException("La nouvelle adresse doit commencer après l'adresse courante");
                    }
                    previous.setDateFin(request.dateDebut().minusDays(1));
                    adresseRepository.save(previous);
                });
        return AdresseResponse.from(adresseRepository.save(EmployeAdresse.builder().employe(getEmploye(id))
                .type(request.type()).adresse(request.adresse()).ville(request.ville()).dateDebut(request.dateDebut()).build()));
    }

    @Transactional
    public ContactResponse addContact(Long id, ContactRequest request, Utilisateur utilisateur) {
        ensureEditable(id, utilisateur);
        validateType(request.type(), "TELEPHONE", "EMAIL");
        if ("EMAIL".equals(request.type()) && !request.valeur().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) throw new IllegalArgumentException("Email invalide");
        if ("TELEPHONE".equals(request.type()) && !request.valeur().matches("^[+0-9][0-9 .-]{7,20}$")) throw new IllegalArgumentException("Téléphone invalide");
        if (request.principal()) contactRepository.findByEmployeIdOrderByDateDebutDesc(id).stream().filter(c -> request.type().equals(c.getType()) && Boolean.TRUE.equals(c.getPrincipal()) && c.getDateFin() == null).forEach(c -> c.setPrincipal(false));
        return ContactResponse.from(contactRepository.save(EmployeContact.builder().employe(getEmploye(id))
                .type(request.type()).valeur(request.valeur()).principal(request.principal()).dateDebut(request.dateDebut()).build()));
    }

    @Transactional
    public ContactUrgenceResponse addEmergencyContact(Long id, ContactUrgenceRequest request, Utilisateur utilisateur) {
        ensureEditable(id, utilisateur);
        if (request.principal()) {
            urgenceRepository.findByEmployeIdOrderByPrincipalDescIdAsc(id)
                    .stream().filter(c -> Boolean.TRUE.equals(c.getPrincipal()))
                    .forEach(c -> c.setPrincipal(false));
        }
        return ContactUrgenceResponse.from(urgenceRepository.save(ContactUrgence.builder()
                .employe(getEmploye(id)).nom(request.nom()).lienParente(request.lienParente())
                .telephone(request.telephone()).principal(request.principal()).build()));
    }

    @Transactional
    public AyantDroitResponse addDependent(Long id, AyantDroitRequest request, Utilisateur utilisateur) {
        ensureEditable(id, utilisateur);
        if (request.dateNaissance() != null && request.dateNaissance().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La date de naissance est invalide");
        }
        return AyantDroitResponse.from(ayantDroitRepository.save(AyantDroit.builder()
                .employe(getEmploye(id)).nom(request.nom()).lien(request.lien())
                .dateNaissance(request.dateNaissance()).build()));
    }

    @Transactional
    public DemandeModificationResponse requestModification(ModificationRequest request, Utilisateur utilisateur) {
        Employe e = employeeForUser(utilisateur);
        return DemandeModificationResponse.from(demandeRepository.save(DemandeModification.builder()
                .employe(e).demandeur(utilisateur).champ(request.champ()).ancienneValeur(request.ancienneValeur())
                .nouvelleValeur(request.nouvelleValeur()).commentaire(request.commentaire()).build()));
    }

    @Transactional(readOnly = true)
    public List<DemandeModificationResponse> pendingModifications(Utilisateur utilisateur) {
        requireRole(utilisateur, "RH", "ADMIN");
        return demandeRepository.findByStatutOrderByDateDemandeAsc(StatutDemandeModification.EN_ATTENTE).stream().map(DemandeModificationResponse::from).toList();
    }

    @Transactional
    public DemandeModificationResponse decideModification(Long requestId, boolean approve, DecisionModificationRequest decision, Utilisateur utilisateur) {
        requireRole(utilisateur, "RH", "ADMIN");
        DemandeModification d = demandeRepository.findById(requestId).orElseThrow(() -> new ResourceNotFoundException("Demande de modification", requestId));
        if (d.getStatut() != StatutDemandeModification.EN_ATTENTE) throw new IllegalStateException("Demande déjà traitée");
        if (approve) applyModification(d);
        d.setStatut(approve ? StatutDemandeModification.APPROUVEE : StatutDemandeModification.REJETEE);
        d.setCommentaire(decision == null ? null : decision.commentaire()); d.setDecideur(utilisateur); d.setDateDecision(java.time.LocalDateTime.now());
        DemandeModificationResponse response = DemandeModificationResponse.from(demandeRepository.save(d));
        String decisionLabel = approve ? "approuvée" : "rejetée";
        String message = "Votre demande de modification du champ « " + d.getChamp() + " » a été " + decisionLabel
                + (decision != null && decision.commentaire() != null && !decision.commentaire().isBlank()
                        ? " : " + decision.commentaire().trim() : ".");
        notificationService.notifyUserForPath(d.getDemandeur(), message, "/mon-dossier");
        return response;
    }

    private void applyModification(DemandeModification d) {
        Employe e = d.getEmploye();
        switch (d.getChamp()) {
            case "telephone" -> e.getCandidat().setTelephone(d.getNouvelleValeur());
            case "adresse" -> {
                e.getCandidat().setAdresse(d.getNouvelleValeur());
                saveApprovedAddress(e, d.getNouvelleValeur());
            }
            case "nationalite" -> e.setNationalite(d.getNouvelleValeur());
            case "situationFamiliale" -> e.setSituationFamiliale(d.getNouvelleValeur());
            default -> throw new IllegalArgumentException("Champ non modifiable : " + d.getChamp());
        }
        employeRepository.save(e);
    }

    private List<AdresseResponse> addressesForDossier(Employe employe) {
        List<AdresseResponse> addresses = adresseRepository.findByEmployeIdOrderByDateDebutDesc(employe.getId())
                .stream().map(AdresseResponse::from).collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        String candidateAddress = employe.getCandidat() == null ? null : employe.getCandidat().getAdresse();
        if (candidateAddress != null && !candidateAddress.isBlank()
                && addresses.stream().noneMatch(address -> candidateAddress.trim().equalsIgnoreCase(address.adresse().trim())
                        && address.dateFin() == null)) {
            addresses.add(new AdresseResponse(null, "PRINCIPALE", candidateAddress, null,
                    employe.getDateEmbauche() == null ? LocalDate.now() : employe.getDateEmbauche(), null));
        }
        return addresses;
    }

    private void saveApprovedAddress(Employe employe, String value) {
        LocalDate today = LocalDate.now();
        List<EmployeAdresse> currentAddresses = adresseRepository.findByEmployeIdOrderByDateDebutDesc(employe.getId())
                .stream().filter(address -> "PRINCIPALE".equals(address.getType()) && address.getDateFin() == null).toList();
        for (EmployeAdresse current : currentAddresses) {
            if (!today.isBefore(current.getDateDebut()) && today.equals(current.getDateDebut())) {
                current.setAdresse(value);
                current.setVille(null);
                adresseRepository.save(current);
                return;
            }
            if (today.isAfter(current.getDateDebut())) {
                current.setDateFin(today.minusDays(1));
                adresseRepository.save(current);
            }
        }
        adresseRepository.save(EmployeAdresse.builder().employe(employe).type("PRINCIPALE")
                .adresse(value).dateDebut(today).build());
    }

    private Employe employeeForUser(Utilisateur user) {
        return employeRepository.findAll().stream().filter(e -> e.getCandidat() != null && user.getEmail().equalsIgnoreCase(e.getCandidat().getEmail())).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Employé du compte", user.getId()));
    }

    private void ensureEmployeeAccess(Long id, Utilisateur user) {
        String role = user.getRole() == null ? null : user.getRole().getCode();
        if ("EMPLOYE".equals(role) && !employeeForUser(user).getId().equals(id)) {
            throw new AccessDeniedException("Accès au dossier refusé");
        }
        if ("MANAGER".equals(role) && !isTeamMember(id, user)) {
            throw new AccessDeniedException("Accès limité à votre équipe");
        }
        ensureAccess(user);
    }
    private void ensureEditable(Long id, Utilisateur user) { if ("EMPLOYE".equals(user.getRole().getCode()) && !employeeForUser(user).getId().equals(id)) throw new AccessDeniedException("Modification refusée"); ensureAccess(user); }
    private void requireRole(Utilisateur u, String... roles) { if (u.getRole() == null || java.util.Arrays.stream(roles).noneMatch(r -> r.equals(u.getRole().getCode()))) throw new AccessDeniedException("Rôle insuffisant"); }
    private void validateType(String value, String... allowed) { if (java.util.Arrays.stream(allowed).noneMatch(value::equals)) throw new IllegalArgumentException("Type invalide"); }
    private void validateBirthDate(LocalDate date) { if (date.isAfter(LocalDate.now().minusYears(16)) || date.isBefore(LocalDate.now().minusYears(100))) throw new IllegalArgumentException("Date de naissance incompatible avec l'âge légal de travail"); }

    private EmployeResponse toResponse(Employe employe) {
        Affectation affectation = affectationRepository
                .findFirstByEmployeIdAndDateFinIsNullOrderByDateAffectationDesc(employe.getId())
                .orElse(null);
        var contrat = contratRepository.findFirstByEmployeIdOrderByDateCreationDesc(employe.getId())
                .orElse(null);
        return EmployeResponse.from(
                employe,
                affectation == null ? null : AffectationResponse.from(affectation),
                contrat == null ? null : ContratResponse.from(contrat)
        );
    }

    private Employe getEmploye(Long id) {
        return employeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", id));
    }

    private void ensureAccess(Utilisateur utilisateur) {
        String role = utilisateur.getRole() == null ? null : utilisateur.getRole().getCode();
        if (!"RH".equals(role) && !"ADMIN".equals(role) && !"MANAGER".equals(role) && !"EMPLOYE".equals(role)) {
            throw new AccessDeniedException("Vous n'avez pas accès à cette ressource");
        }
    }

    private boolean isTeamMember(Long employeeId, Utilisateur manager) {
        Employe managerEmployee = employeRepository.findAll().stream()
                .filter(e -> e.getCandidat() != null
                        && manager.getEmail().equalsIgnoreCase(e.getCandidat().getEmail()))
                .findFirst().orElse(null);
        if (managerEmployee == null) return false;
        return affectationRepository.findFirstByEmployeIdAndDateFinIsNullOrderByDateAffectationDesc(employeeId)
                .map(a -> a.getManager() != null && a.getManager().getId().equals(managerEmployee.getId()))
                .orElse(false);
    }

    private boolean canReadSensitive(Utilisateur user) {
        String role = user.getRole() == null ? null : user.getRole().getCode();
        return "RH".equals(role) || "ADMIN".equals(role) || "AUDITEUR".equals(role)
                || "EMPLOYE".equals(role);
    }

    private EmployeResponse toResponseFor(Employe employe, Utilisateur user) {
        EmployeResponse response = toResponse(employe);
        if (canReadSensitive(user)) return response;
        return new EmployeResponse(response.id(), response.candidatId(), response.matricule(),
                response.nom(), response.prenom(), response.email(), response.telephone(), response.statut(),
                response.dateEmbauche(), null, null, null, null, null, null, null, null,
                response.dateSortie(), response.motifSortie(), response.poste(), response.departement(),
                response.affectationCourante(), response.contratCourant());
    }
}
