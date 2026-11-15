package tovo.gestion.rh.embauche.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tovo.gestion.rh.candidature.entity.Candidature;
import tovo.gestion.rh.candidature.entity.StatutCandidature;
import tovo.gestion.rh.candidature.repository.CandidatureRepository;
import tovo.gestion.rh.common.exception.ResourceNotFoundException;
import tovo.gestion.rh.demanderecrutement.entity.DemandeRecrutement;
import tovo.gestion.rh.embauche.dto.*;
import tovo.gestion.rh.embauche.entity.PropositionEmbauche;
import tovo.gestion.rh.embauche.entity.StatutProposition;
import tovo.gestion.rh.embauche.repository.PropositionEmbaucheRepository;
import tovo.gestion.rh.utilisateur.entity.Utilisateur;
import tovo.gestion.rh.workflow.service.NotificationService;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PropositionEmbaucheService {
    private static final int DUREE_VALIDITE_JOURS = 15;
    private static final int MAX_VERSIONS = 3;

    private final CandidatureRepository candidatureRepository;
    private final PropositionEmbaucheRepository propositionRepository;
    private final NotificationService notificationService;
    private final CreationEmployeService creationEmployeService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public PropositionResponse creer(Long candidatureId, CreerPropositionRequest request,
                                     Utilisateur rh) {
        requireRh(rh);
        Candidature candidature = getCandidature(candidatureId);
        if (candidature.getStatut() != StatutCandidature.RETENU) {
            throw new IllegalStateException("Une proposition est réservée à une candidature RETENU");
        }
        assertBudget(candidature, request.salaire());
        if (propositionRepository.findByCandidatureIdAndStatutIn(
                candidatureId, List.of(StatutProposition.EN_ATTENTE, StatutProposition.NEGOCIATION)).isPresent()) {
            throw new IllegalStateException("Une proposition active existe déjà pour cette candidature");
        }

        String token = generateToken();
        PropositionEmbauche proposition = PropositionEmbauche.builder()
                .candidature(candidature)
                .emisePar(rh)
                .dateDebut(request.dateDebut())
                .lieu(request.lieu().trim())
                .salaire(request.salaire())
                .typeContrat(request.typeContrat())
                .jetonAccesHash(hash(token))
                .jetonExpireLe(LocalDateTime.now().plusDays(DUREE_VALIDITE_JOURS))
                .build();
        proposition = propositionRepository.save(proposition);
        notifyManager(candidature, "Une proposition d'embauche est disponible pour votre candidat retenu.");
        return PropositionResponse.from(proposition, token);
    }

    @Transactional
    public PropositionResponse repondre(Long id, RepondrePropositionRequest request) {
        PropositionEmbauche proposition = get(id);
        validateToken(proposition, request.jeton());
        if (proposition.getStatut() != StatutProposition.EN_ATTENTE) {
            throw new IllegalStateException("Cette proposition ne peut plus recevoir de réponse");
        }
        proposition.setDateReponse(LocalDateTime.now());
        proposition.setRemarqueCandidat(request.remarque());
        proposition.setSouhaitsCandidat(request.souhaits());
        proposition.setJetonAccesHash(null);
        proposition.setJetonExpireLe(null);
        proposition.setStatut(switch (request.reponse()) {
            case ACCEPTER -> StatutProposition.ACCEPTE;
            case REFUSER -> StatutProposition.REFUSE;
            case NEGOCIER -> StatutProposition.NEGOCIATION;
        });
        proposition = propositionRepository.save(proposition);
        if (proposition.getStatut() == StatutProposition.ACCEPTE) {
            creationEmployeService.creerDepuisAcceptation(proposition);
        }
        notifyManager(proposition.getCandidature(), "Le candidat a répondu à la proposition #" + id + ".");
        return PropositionResponse.from(proposition, null);
    }

    @Transactional(readOnly = true)
    public PropositionResponse consulterPubliquement(Long id, String jeton) {
        PropositionEmbauche proposition = get(id);
        validateToken(proposition, jeton);
        return PropositionResponse.from(proposition, null);
    }

    @Transactional(readOnly = true)
    public List<PropositionResponse> historiquePublique(Long id, String jeton) {
        PropositionEmbauche proposition = get(id);
        validateToken(proposition, jeton);
        return propositionRepository.findByCandidatureIdOrderByVersionAsc(proposition.getCandidature().getId())
                .stream().map(item -> PropositionResponse.from(item, null)).toList();
    }

    @Transactional
    public PropositionResponse ajuster(Long id, AjusterPropositionRequest request, Utilisateur rh) {
        requireRh(rh);
        PropositionEmbauche precedente = get(id);
        if (precedente.getStatut() != StatutProposition.NEGOCIATION) {
            throw new IllegalStateException("Seule une proposition en négociation peut être ajustée");
        }
        if (precedente.getVersion() >= MAX_VERSIONS) {
            throw new IllegalStateException("Le nombre maximal de tours de négociation est atteint");
        }
        assertBudget(precedente.getCandidature(), request.salaire());
        String token = generateToken();
        PropositionEmbauche suivante = PropositionEmbauche.builder()
                .candidature(precedente.getCandidature())
                .version(precedente.getVersion() + 1)
                .propositionPrecedente(precedente)
                .emisePar(rh)
                .dateDebut(request.dateDebut())
                .lieu(request.lieu().trim())
                .salaire(request.salaire())
                .typeContrat(request.typeContrat())
                .jetonAccesHash(hash(token))
                .jetonExpireLe(LocalDateTime.now().plusDays(DUREE_VALIDITE_JOURS))
                .build();
        suivante = propositionRepository.save(suivante);
        notifyManager(suivante.getCandidature(), "Une nouvelle version de la proposition #" + suivante.getId()
                + " est disponible.");
        return PropositionResponse.from(suivante, token);
    }

    @Transactional
    public PropositionResponse rejeter(Long id, Utilisateur rh) {
        requireRh(rh);
        PropositionEmbauche proposition = get(id);
        if (proposition.getStatut() != StatutProposition.NEGOCIATION) {
            throw new IllegalStateException("Seule une proposition en négociation peut être rejetée");
        }
        proposition.setStatut(StatutProposition.REFUSE);
        proposition.setDateReponse(LocalDateTime.now());
        proposition.setJetonAccesHash(null);
        proposition.setJetonExpireLe(null);
        return PropositionResponse.from(propositionRepository.save(proposition), null);
    }

    @Transactional(readOnly = true)
    public List<PropositionResponse> historique(Long candidatureId, Utilisateur utilisateur) {
        requireRhOuManager(utilisateur, getCandidature(candidatureId));
        return propositionRepository.findByCandidatureIdOrderByVersionAsc(candidatureId)
                .stream().map(p -> PropositionResponse.from(p, null)).toList();
    }

    private void validateToken(PropositionEmbauche proposition, String token) {
        if (proposition.getJetonAccesHash() == null
                || proposition.getJetonExpireLe() == null
                || proposition.getJetonExpireLe().isBefore(LocalDateTime.now())
                || !MessageDigest.isEqual(
                    proposition.getJetonAccesHash().getBytes(StandardCharsets.UTF_8),
                    hash(token).getBytes(StandardCharsets.UTF_8))) {
            if (proposition.getStatut() == StatutProposition.EN_ATTENTE
                    && proposition.getJetonExpireLe() != null
                    && proposition.getJetonExpireLe().isBefore(LocalDateTime.now())) {
                proposition.setStatut(StatutProposition.EXPIREE);
                proposition.setJetonAccesHash(null);
                propositionRepository.save(proposition);
            }
            throw new AccessDeniedException("Le lien de réponse est invalide ou expiré");
        }
    }

    private void assertBudget(Candidature candidature, BigDecimal salaire) {
        DemandeRecrutement demande = candidature.getOffre().getDemandeRecrutement();
        if (salaire.compareTo(demande.getBudget()) > 0) {
            throw new IllegalArgumentException("Le salaire proposé dépasse le budget de la demande");
        }
    }

    private void notifyManager(Candidature candidature, String message) {
        notificationService.notifyUser(
                candidature.getOffre().getDemandeRecrutement().getDemandeur(), message,
                candidature.getOffre().getDemandeRecrutement().getId());
    }

    private Candidature getCandidature(Long id) {
        return candidatureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Candidature", id));
    }

    private PropositionEmbauche get(Long id) {
        return propositionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proposition d'embauche", id));
    }

    private void requireRh(Utilisateur utilisateur) {
        if (!"RH".equals(utilisateur.getRole().getCode()) && !"ADMIN".equals(utilisateur.getRole().getCode())) {
            throw new AccessDeniedException("Cette action est réservée à la RH");
        }
    }

    private void requireRhOuManager(Utilisateur utilisateur, Candidature candidature) {
        if ("RH".equals(utilisateur.getRole().getCode()) || "ADMIN".equals(utilisateur.getRole().getCode())) {
            return;
        }
        if ("MANAGER".equals(utilisateur.getRole().getCode())
                && candidature.getOffre().getDemandeRecrutement().getDemandeur().getId()
                .equals(utilisateur.getId())) {
            return;
        }
        throw new AccessDeniedException("Vous ne pouvez pas consulter ces propositions");
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algorithme de hachage indisponible", e);
        }
    }
}
