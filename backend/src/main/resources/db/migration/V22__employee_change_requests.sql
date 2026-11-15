CREATE TABLE demande_modification (
    id BIGSERIAL PRIMARY KEY,
    employe_id BIGINT NOT NULL,
    demandeur_id INT NOT NULL,
    champ VARCHAR(80) NOT NULL,
    ancienne_valeur TEXT,
    nouvelle_valeur TEXT NOT NULL,
    statut VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE',
    commentaire TEXT,
    date_demande TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_decision TIMESTAMP,
    decideur_id INT,
    CONSTRAINT fk_demande_modification_employe FOREIGN KEY (employe_id) REFERENCES employe(id),
    CONSTRAINT fk_demande_modification_demandeur FOREIGN KEY (demandeur_id) REFERENCES utilisateur(id),
    CONSTRAINT fk_demande_modification_decideur FOREIGN KEY (decideur_id) REFERENCES utilisateur(id),
    CONSTRAINT chk_demande_modification_statut CHECK (statut IN ('EN_ATTENTE', 'APPROUVEE', 'REJETEE'))
);

CREATE INDEX idx_demande_modification_statut ON demande_modification(statut, date_demande);
CREATE INDEX idx_demande_modification_employe ON demande_modification(employe_id);
