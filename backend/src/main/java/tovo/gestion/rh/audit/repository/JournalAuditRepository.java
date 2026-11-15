package tovo.gestion.rh.audit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tovo.gestion.rh.audit.entity.JournalAudit;

public interface JournalAuditRepository extends JpaRepository<JournalAudit, Long>, JournalAuditSearch {
}
