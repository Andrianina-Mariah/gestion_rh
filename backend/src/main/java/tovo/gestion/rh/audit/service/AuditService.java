package tovo.gestion.rh.audit.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tovo.gestion.rh.audit.dto.AuditResponse;
import tovo.gestion.rh.audit.entity.JournalAudit;
import tovo.gestion.rh.audit.repository.JournalAuditRepository;
import tovo.gestion.rh.utilisateur.entity.Utilisateur;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final JournalAuditRepository repository;

    @Transactional
    public void record(Utilisateur utilisateur, String action, String entite, Long entiteId,
                       String ancienneValeur, String nouvelleValeur, String adresseIp) {
        repository.save(JournalAudit.builder()
                .utilisateur(utilisateur)
                .role(utilisateur == null || utilisateur.getRole() == null ? null : utilisateur.getRole().getCode())
                .action(action).entite(entite).entiteId(entiteId)
                .ancienneValeur(ancienneValeur).nouvelleValeur(nouvelleValeur).adresseIp(adresseIp)
                .build());
    }

    @Transactional(readOnly = true)
    public List<AuditResponse> search(Integer utilisateurId, String entite, String action,
                                      LocalDateTime depuis, LocalDateTime jusqua) {
        String normalizedEntity = entite == null || entite.isBlank() ? null : entite.trim().toUpperCase(java.util.Locale.ROOT);
        String normalizedAction = action == null || action.isBlank() ? null : action.trim().toUpperCase(java.util.Locale.ROOT);
        return repository.search(utilisateurId, normalizedEntity, normalizedAction, depuis, jusqua).stream()
                .map(a -> new AuditResponse(a.getId(), a.getDateHeure(),
                        a.getUtilisateur() == null ? null : a.getUtilisateur().getId(),
                        a.getRole(), a.getAction(), a.getEntite(), a.getEntiteId(),
                        a.getAncienneValeur(), a.getNouvelleValeur(), a.getAdresseIp()))
                .toList();
    }
}
