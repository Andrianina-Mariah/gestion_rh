ALTER TABLE anomalie_pointage DROP CONSTRAINT chk_anomalie_pointage_type;
ALTER TABLE anomalie_pointage ADD CONSTRAINT chk_anomalie_pointage_type CHECK (type IN (
    'ENTREE_MANQUANTE', 'SORTIE_MANQUANTE', 'DOUBLON', 'HORS_PLANNING',
    'DUREE_EXCESSIVE', 'RETARD_NON_JUSTIFIE', 'HORODATAGE_INVALIDE', 'ABSENCE_SANS_CONGE'
));
ALTER TABLE anomalie_pointage
    ADD COLUMN traitee_par_id INT REFERENCES utilisateur(id),
    ADD COLUMN traitee_le TIMESTAMPTZ;

ALTER TABLE periode_temps
    ADD COLUMN validee_par_id INT REFERENCES utilisateur(id),
    ADD COLUMN date_validation TIMESTAMPTZ,
    ADD COLUMN motif_derogation TEXT,
    ADD COLUMN reouverte_par_id INT REFERENCES utilisateur(id),
    ADD COLUMN date_reouverture TIMESTAMPTZ,
    ADD COLUMN motif_reouverture TEXT,
    ADD COLUMN date_export TIMESTAMPTZ,
    ADD CONSTRAINT chk_periode_validation_meta CHECK (
        (statut = 'OUVERTE') OR (validee_par_id IS NOT NULL AND date_validation IS NOT NULL)
    );

ALTER TABLE regle_temps ADD COLUMN rappel_anomalie_jours INT NOT NULL DEFAULT 3 CHECK (rappel_anomalie_jours > 0);

CREATE TABLE absence_a_qualifier (
    id BIGSERIAL PRIMARY KEY,
    anomalie_id BIGINT NOT NULL UNIQUE REFERENCES anomalie_pointage(id),
    employe_id BIGINT NOT NULL REFERENCES employe(id),
    date DATE NOT NULL,
    minutes INT NOT NULL CHECK (minutes > 0),
    statut VARCHAR(20) NOT NULL DEFAULT 'A_QUALIFIER' CHECK (statut IN ('A_QUALIFIER', 'QUALIFIEE')),
    date_creation TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_absence_a_qualifier_statut_date ON absence_a_qualifier(statut, date);

CREATE TABLE anomalie_rappel (
    id BIGSERIAL PRIMARY KEY,
    anomalie_id BIGINT NOT NULL REFERENCES anomalie_pointage(id),
    date_rappel DATE NOT NULL,
    CONSTRAINT uq_anomalie_rappel_jour UNIQUE (anomalie_id, date_rappel)
);
