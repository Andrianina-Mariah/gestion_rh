package tovo.gestion.rh.audit.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;
import tovo.gestion.rh.audit.entity.JournalAudit;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class JournalAuditSearchImpl implements JournalAuditSearch {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<JournalAudit> search(Integer utilisateurId, String entite, String action,
                                     LocalDateTime depuis, LocalDateTime jusqua) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<JournalAudit> query = builder.createQuery(JournalAudit.class);
        Root<JournalAudit> audit = query.from(JournalAudit.class);
        List<Predicate> filters = new ArrayList<>();

        if (utilisateurId != null) {
            filters.add(builder.equal(audit.join("utilisateur", JoinType.LEFT).get("id"), utilisateurId));
        }
        if (entite != null && !entite.isBlank()) {
            filters.add(builder.equal(builder.upper(audit.get("entite")), entite.trim().toUpperCase(java.util.Locale.ROOT)));
        }
        if (action != null && !action.isBlank()) {
            filters.add(builder.equal(builder.upper(audit.get("action")), action.trim().toUpperCase(java.util.Locale.ROOT)));
        }
        if (depuis != null) filters.add(builder.greaterThanOrEqualTo(audit.get("dateHeure"), depuis));
        if (jusqua != null) filters.add(builder.lessThanOrEqualTo(audit.get("dateHeure"), jusqua));

        query.select(audit).where(filters.toArray(Predicate[]::new)).orderBy(builder.desc(audit.get("dateHeure")));
        return entityManager.createQuery(query).getResultList();
    }
}
