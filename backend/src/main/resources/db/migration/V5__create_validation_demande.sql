CREATE TABLE validation_demande (
    id SERIAL PRIMARY KEY,
    demande_recrutement_id INT NOT NULL,
    etape VARCHAR(50) NOT NULL,
    validateur_id INT NOT NULL,
    statut VARCHAR(30) NOT NULL,
    commentaire TEXT,
    date_validation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_validation_demande FOREIGN KEY (demande_recrutement_id) REFERENCES demande_recrutement(id),
    CONSTRAINT fk_validation_validateur FOREIGN KEY (validateur_id) REFERENCES utilisateur(id)
);

CREATE INDEX idx_validation_demande_id ON validation_demande(demande_recrutement_id);