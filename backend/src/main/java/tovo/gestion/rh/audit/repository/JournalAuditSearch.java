package tovo.gestion.rh.audit.repository;

import tovo.gestion.rh.audit.entity.JournalAudit;
import java.time.LocalDateTime;
import java.util.List;

public interface JournalAuditSearch {
    List<JournalAudit> search(Integer utilisateurId, String entite, String action,
                              LocalDateTime depuis, LocalDateTime jusqua);
}
