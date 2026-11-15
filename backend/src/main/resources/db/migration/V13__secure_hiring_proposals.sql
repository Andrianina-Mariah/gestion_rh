ALTER TABLE proposition_embauche
    ADD COLUMN souhaits_candidat TEXT,
    ADD COLUMN jeton_acces_hash VARCHAR(64),
    ADD COLUMN jeton_expire_le TIMESTAMP;

ALTER TABLE proposition_embauche
    DROP CONSTRAINT chk_proposition_statut;

ALTER TABLE proposition_embauche
    ADD CONSTRAINT chk_proposition_statut
        CHECK (statut IN ('EN_ATTENTE', 'ACCEPTE', 'REFUSE', 'NEGOCIATION', 'EXPIREE'));

CREATE UNIQUE INDEX uq_proposition_jeton_acces
    ON proposition_embauche(jeton_acces_hash)
    WHERE jeton_acces_hash IS NOT NULL;

CREATE INDEX idx_proposition_jeton_expiration
    ON proposition_embauche(jeton_expire_le);
