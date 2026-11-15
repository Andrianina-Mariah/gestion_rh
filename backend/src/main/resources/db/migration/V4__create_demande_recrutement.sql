CREATE TABLE demande_recrutement (
    id SERIAL PRIMARY KEY,
    poste_id INT NOT NULL,
    demandeur_id INT NOT NULL,
    validateur_id INT,
    nombre_postes INT NOT NULL DEFAULT 1,
    type VARCHAR(20) NOT NULL,
    motif TEXT,
    budget NUMERIC(12, 2) NOT NULL,
    date_souhaitee DATE NOT NULL,
    statut VARCHAR(50) NOT NULL DEFAULT 'BROUILLON',
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_modification TIMESTAMP,
    employe_remplace_id INT,

    CONSTRAINT fk_demande_poste FOREIGN KEY (poste_id) REFERENCES poste(id),
    CONSTRAINT fk_demande_demandeur FOREIGN KEY (demandeur_id) REFERENCES utilisateur(id),
    CONSTRAINT fk_demande_validateur FOREIGN KEY (validateur_id) REFERENCES utilisateur(id),
    CONSTRAINT chk_type CHECK (type IN ('CREATION', 'REMPLACEMENT')),
    CONSTRAINT chk_nombre_postes CHECK (nombre_postes > 0),
    CONSTRAINT chk_budget CHECK (budget >= 0)
);

CREATE INDEX idx_demande_statut ON demande_recrutement(statut);
CREATE INDEX idx_demande_demandeur_id ON demande_recrutement(demandeur_id);
CREATE INDEX idx_demande_poste_id ON demande_recrutement(poste_id);