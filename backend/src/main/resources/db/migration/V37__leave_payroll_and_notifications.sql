ALTER TABLE variable_paie DROP CONSTRAINT chk_variable_paie_type;
ALTER TABLE variable_paie ADD CONSTRAINT chk_variable_paie_type CHECK (type IN (
    'HEURES_NORMALES', 'HEURES_SUP_30', 'HEURES_SUP_50', 'NUIT',
    'DIMANCHE_FERIE', 'RETARD', 'ABSENCE', 'JOURS_CONGE_PAYE',
    'JOURS_CONGE_SANS_SOLDE', 'JOURS_ABSENCE_INJUSTIFIEE',
    'JOURS_MALADIE_PAYEE', 'JOURS_MALADIE_NON_PAYEE'
));
ALTER TABLE variable_paie ADD COLUMN quantite_jours NUMERIC(8,2);
ALTER TABLE variable_paie ADD CONSTRAINT chk_variable_paie_quantite_jours
    CHECK (quantite_jours IS NULL OR quantite_jours >= 0);

ALTER TABLE politique_demande_conge
    ADD COLUMN rappel_validation_jours INT NOT NULL DEFAULT 2 CHECK (rappel_validation_jours >= 1),
    ADD COLUMN seuil_solde_faible_jours NUMERIC(6,2) NOT NULL DEFAULT 2 CHECK (seuil_solde_faible_jours >= 0);

CREATE TABLE demande_rappel_validation (
    id BIGSERIAL PRIMARY KEY,
    demande_id BIGINT NOT NULL REFERENCES demande_conge(id),
    role_cible VARCHAR(10) NOT NULL CHECK (role_cible IN ('MANAGER','RH')),
    date_rappel DATE NOT NULL,
    CONSTRAINT uq_demande_rappel_validation UNIQUE(demande_id,role_cible,date_rappel)
);
