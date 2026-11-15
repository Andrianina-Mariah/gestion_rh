DROP INDEX uq_proposition_active_candidature;

CREATE UNIQUE INDEX uq_proposition_active_candidature
    ON proposition_embauche(candidature_id)
    WHERE statut = 'EN_ATTENTE';
