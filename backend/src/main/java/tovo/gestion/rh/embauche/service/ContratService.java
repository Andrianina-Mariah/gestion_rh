package tovo.gestion.rh.embauche.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tovo.gestion.rh.common.exception.ResourceNotFoundException;
import tovo.gestion.rh.embauche.dto.ContratResponse;
import tovo.gestion.rh.embauche.dto.AccesEmployeResponse;
import tovo.gestion.rh.embauche.dto.SignerContratRequest;
import tovo.gestion.rh.embauche.dto.ValiderContratRequest;
import tovo.gestion.rh.embauche.entity.Contrat;
import tovo.gestion.rh.embauche.entity.Employe;
import tovo.gestion.rh.embauche.entity.StatutContrat;
import tovo.gestion.rh.embauche.entity.StatutEmploye;
import tovo.gestion.rh.embauche.repository.ContratRepository;
import tovo.gestion.rh.embauche.repository.EmployeRepository;
import tovo.gestion.rh.utilisateur.entity.Role;
import tovo.gestion.rh.utilisateur.entity.Utilisateur;
import tovo.gestion.rh.utilisateur.repository.RoleRepository;
import tovo.gestion.rh.utilisateur.repository.UtilisateurRepository;
import tovo.gestion.rh.onboarding.entity.TypeOnboardingTache;
import tovo.gestion.rh.onboarding.service.OnboardingService;
import tovo.gestion.rh.conge.service.CongeSoldeService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContratService {

    private final ContratRepository contratRepository;
    private final EmployeRepository employeRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final OnboardingService onboardingService;
    private final CongeSoldeService congeSoldeService;

    @Transactional(readOnly = true)
    public List<ContratResponse> findByEmployeId(Long employeId) {
        Employe employe = getEmploye(employeId);
        return contratRepository.findByEmployeIdOrderByDateDebutAsc(employe.getId())
                .stream()
                .map(ContratResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ContratResponse> findEcheances(int jours) {
        if (jours < 0 || jours > 3650) {
            throw new IllegalArgumentException("Le nombre de jours doit être compris entre 0 et 3650");
        }
        LocalDate aujourdHui = LocalDate.now();
        return contratRepository.findByDateFinIsNotNullAndDateFinBetweenOrderByDateFinAsc(
                        aujourdHui, aujourdHui.plusDays(jours))
                .stream()
                .filter(contrat -> contrat.getStatut() == StatutContrat.VALIDE)
                .map(ContratResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ContratResponse findById(Long id) {
        return ContratResponse.from(getContrat(id));
    }

    @Transactional
    public ContratResponse signer(Long id, SignerContratRequest request) {
        Contrat contrat = getContrat(id);
        if (contrat.getStatut() == StatutContrat.VALIDE) {
            throw new IllegalStateException("Le contrat est déjà validé et n'est plus modifiable");
        }
        if (request != null && request.documentUrl() != null && !request.documentUrl().isBlank()) {
            contrat.setContratUrl(request.documentUrl());
        }
        if (contrat.getStatut() == StatutContrat.BROUILLON) {
            contrat.setStatut(StatutContrat.CREE);
        }
        contrat.setStatut(StatutContrat.SIGNE);
        return ContratResponse.from(contratRepository.save(contrat));
    }

    @Transactional
    public ContratResponse valider(Long id, ValiderContratRequest request, Utilisateur utilisateur) {
        requireRh(utilisateur);
        Contrat contrat = getContrat(id);
        if (contrat.getStatut() != StatutContrat.SIGNE) {
            throw new IllegalStateException("Le contrat doit être signé avant validation RH");
        }
        if (request != null && request.documentUrl() != null && !request.documentUrl().isBlank()) {
            contrat.setContratUrl(request.documentUrl());
        }
        contrat.setStatut(StatutContrat.VALIDE);
        contrat = contratRepository.save(contrat);
        terminerContratPrecedent(contrat);

        Employe employe = contrat.getEmploye();
        employe.setStatut(StatutEmploye.ACTIF);
        employeRepository.save(employe);
        congeSoldeService.initialiserEmploye(employe.getId());
        creerCompteEmployeSiNecessaire(employe);
        onboardingService.marquerTypeFait(employe.getId(), TypeOnboardingTache.CONTRAT,
                "Contrat validé par la RH.");
        return ContratResponse.from(contrat);
    }

    @Transactional
    public AccesEmployeResponse reinitialiserAcces(Long contratId, Utilisateur utilisateur) {
        requireRh(utilisateur);
        Contrat contrat = getContrat(contratId);
        if (contrat.getStatut() != StatutContrat.VALIDE) {
            throw new IllegalStateException("L'accès salarié peut être activé après validation du contrat");
        }
        Employe employe = contrat.getEmploye();
        String email = employe.getCandidat().getEmail();
        Role roleEmploye = roleRepository.findByCode("EMPLOYE")
                .orElseThrow(() -> new IllegalStateException("Le rôle EMPLOYE est introuvable"));
        Utilisateur compte = utilisateurRepository.findByEmail(email).orElse(null);
        if (compte != null && !"EMPLOYE".equals(compte.getRole().getCode())) {
            throw new IllegalStateException("Cette adresse est déjà associée à un compte qui n'est pas salarié");
        }
        String motDePasseTemporaire = UUID.randomUUID().toString();
        if (compte == null) {
            compte = Utilisateur.builder().nom(employe.getCandidat().getNom())
                    .prenom(employe.getCandidat().getPrenom()).email(email).role(roleEmploye).build();
        }
        compte.setMotDePasse(passwordEncoder.encode(motDePasseTemporaire));
        compte.setActif(true);
        compte.setChangementMotDePasseRequis(true);
        utilisateurRepository.save(compte);
        onboardingService.marquerTypeFait(employe.getId(), TypeOnboardingTache.COMPTE,
                "Accès salarié activé par la RH.");
        return new AccesEmployeResponse(email, motDePasseTemporaire);
    }

    @Transactional
    public ContratResponse renouveler(Long id, Utilisateur utilisateur) {
        requireRh(utilisateur);
        Contrat precedent = getContrat(id);
        verifierContratRenouvelable(precedent);
        return ContratResponse.from(contratRepository.save(Contrat.builder()
                .employe(precedent.getEmploye())
                .typeContrat(precedent.getTypeContrat())
                .dateDebut(precedent.getDateFin().plusDays(1))
                .dateFin(calculerDateFin(precedent.getTypeContrat(), precedent.getDateFin().plusDays(1)))
                .salaire(precedent.getSalaire())
                .statut(StatutContrat.BROUILLON)
                .numeroRenouvellement(precedent.getNumeroRenouvellement() + 1)
                .contratPrecedent(precedent)
                .motif("RENOUVELLEMENT")
                .build()));
    }

    @Transactional
    public ContratResponse convertirCdi(Long id, Utilisateur utilisateur) {
        requireRh(utilisateur);
        Contrat precedent = getContrat(id);
        if (precedent.getTypeContrat() != tovo.gestion.rh.embauche.entity.TypeContratPropose.CDD) {
            throw new IllegalStateException("Seul un contrat CDD peut être converti en CDI");
        }
        verifierContratSource(precedent);
        LocalDate dateDebut = precedent.getDateFin().plusDays(1);
        return ContratResponse.from(contratRepository.save(Contrat.builder()
                .employe(precedent.getEmploye())
                .typeContrat(tovo.gestion.rh.embauche.entity.TypeContratPropose.CDI)
                .dateDebut(dateDebut)
                .salaire(precedent.getSalaire())
                .statut(StatutContrat.BROUILLON)
                .numeroRenouvellement(precedent.getNumeroRenouvellement() + 1)
                .contratPrecedent(precedent)
                .motif("PASSAGE_CDI")
                .build()));
    }

    private void verifierContratRenouvelable(Contrat contrat) {
        verifierContratSource(contrat);
        if (contrat.getTypeContrat() == tovo.gestion.rh.embauche.entity.TypeContratPropose.CDI) {
            throw new IllegalStateException("Un contrat CDI ne peut pas être renouvelé");
        }
        if (contrat.getTypeContrat() == tovo.gestion.rh.embauche.entity.TypeContratPropose.CDD
                && contrat.getNumeroRenouvellement() >= 1) {
            throw new IllegalStateException("Un contrat CDD ne peut être renouvelé qu'une seule fois");
        }
    }

    private void verifierContratSource(Contrat contrat) {
        if (contrat.getStatut() != StatutContrat.VALIDE || contrat.getDateFin() == null) {
            throw new IllegalStateException("Le contrat doit être valide et avoir une date de fin");
        }
        if (contratRepository.findFirstByEmployeIdOrderByDateCreationDesc(contrat.getEmploye().getId())
                .map(dernier -> !dernier.getId().equals(contrat.getId())
                        && dernier.getDateDebut().isAfter(contrat.getDateDebut()))
                .orElse(false)) {
            throw new IllegalStateException("Un contrat plus récent existe déjà pour cet employé");
        }
    }

    private LocalDate calculerDateFin(
            tovo.gestion.rh.embauche.entity.TypeContratPropose type, LocalDate debut) {
        return switch (type) {
            case CE -> debut.plusMonths(6);
            case CDD -> debut.plusYears(2);
            case CDI -> null;
        };
    }

    private void terminerContratPrecedent(Contrat contrat) {
        Contrat precedent = contrat.getContratPrecedent();
        if (precedent != null && precedent.getStatut() == StatutContrat.VALIDE) {
            precedent.setStatut(StatutContrat.TERMINE);
            contratRepository.save(precedent);
        }
    }

    private void requireRh(Utilisateur utilisateur) {
        String role = utilisateur.getRole() == null ? null : utilisateur.getRole().getCode();
        if (!"RH".equals(role) && !"ADMIN".equals(role)) {
            throw new AccessDeniedException("Cette action est réservée à la RH");
        }
    }

    private Contrat getContrat(Long id) {
        return contratRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contrat", id));
    }

    private Employe getEmploye(Long id) {
        return employeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé", id));
    }

    private void creerCompteEmployeSiNecessaire(Employe employe) {
        if (employe == null || employe.getCandidat() == null) {
            return;
        }
        String email = employe.getCandidat().getEmail();
        if (email == null || email.isBlank() || utilisateurRepository.existsByEmail(email)) {
            return;
        }
        Role roleEmploye = roleRepository.findByCode("EMPLOYE")
                .orElseThrow(() -> new IllegalStateException("Le rôle EMPLOYE est introuvable"));
        String motDePasse = "ChangeMe123!";
        utilisateurRepository.save(Utilisateur.builder()
                .nom(employe.getCandidat().getNom())
                .prenom(employe.getCandidat().getPrenom())
                .email(email)
                .motDePasse(passwordEncoder.encode(motDePasse))
                .role(roleEmploye)
                .actif(true)
                .changementMotDePasseRequis(true)
                .build());
        onboardingService.marquerTypeFait(employe.getId(), TypeOnboardingTache.COMPTE,
                "Compte utilisateur créé.");
    }
}
