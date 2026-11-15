-- Bloc 7 : types de congés, droits, soldes, demandes, absences et justificatifs.
-- Les valeurs de politique (acquisition, rémunération, débit, justificatifs) sont
-- administrées en données et ne sont pas fixées par cette migration.

CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE type_conge (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    libelle VARCHAR(150) NOT NULL,
    remunere BOOLEAN NOT NULL,
    debite_solde BOOLEAN NOT NULL,
    justificatif_obligatoire BOOLEAN NOT NULL,
    justificatif_apres_jours NUMERIC(6,2),
    duree_max_jours NUMERIC(7,2),
    demi_journee_autorisee BOOLEAN NOT NULL,
    unite_decompte VARCHAR(12) NOT NULL,
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_type_conge_justificatif_apres CHECK (justificatif_apres_jours IS NULL OR justificatif_apres_jours >= 0),
    CONSTRAINT chk_type_conge_duree_max CHECK (duree_max_jours IS NULL OR duree_max_jours > 0),
    CONSTRAINT chk_type_conge_unite CHECK (unite_decompte IN ('OUVRES', 'OUVRABLES'))
);

CREATE TABLE droit_conge (
    id BIGSERIAL PRIMARY KEY,
    type_conge_id BIGINT NOT NULL REFERENCES type_conge(id),
    type_contrat VARCHAR(3),
    regle_acquisition NUMERIC(7,3) NOT NULL,
    periode_acquisition VARCHAR(10) NOT NULL,
    anciennete_min_mois INT NOT NULL,
    plafond_report NUMERIC(7,2) NOT NULL,
    validite_report_mois INT,
    CONSTRAINT chk_droit_conge_acquisition CHECK (regle_acquisition >= 0),
    CONSTRAINT chk_droit_conge_contrat CHECK (type_contrat IS NULL OR type_contrat IN ('CE', 'CDD', 'CDI')),
    CONSTRAINT chk_droit_conge_periode CHECK (periode_acquisition IN ('MOIS', 'ANNEE')),
    CONSTRAINT chk_droit_conge_anciennete CHECK (anciennete_min_mois >= 0),
    CONSTRAINT chk_droit_conge_plafond CHECK (plafond_report >= 0),
    CONSTRAINT chk_droit_conge_validite CHECK (validite_report_mois IS NULL OR validite_report_mois > 0)
);
CREATE UNIQUE INDEX uq_droit_conge_cible ON droit_conge (type_conge_id, COALESCE(type_contrat, '*'), anciennete_min_mois);

CREATE TABLE solde_conge (
    id BIGSERIAL PRIMARY KEY,
    employe_id BIGINT NOT NULL REFERENCES employe(id),
    type_conge_id BIGINT NOT NULL REFERENCES type_conge(id),
    annee SMALLINT NOT NULL,
    acquis NUMERIC(8,2) NOT NULL DEFAULT 0,
    pris NUMERIC(8,2) NOT NULL DEFAULT 0,
    en_attente NUMERIC(8,2) NOT NULL DEFAULT 0,
    reporte NUMERIC(8,2) NOT NULL DEFAULT 0,
    ajuste NUMERIC(8,2) NOT NULL DEFAULT 0,
    CONSTRAINT uq_solde_conge_employe_type_annee UNIQUE (employe_id, type_conge_id, annee),
    CONSTRAINT chk_solde_conge_pris CHECK (pris >= 0),
    CONSTRAINT chk_solde_conge_en_attente CHECK (en_attente >= 0),
    CONSTRAINT chk_solde_conge_acquis CHECK (acquis >= 0),
    CONSTRAINT chk_solde_conge_reporte CHECK (reporte >= 0)
);
CREATE INDEX idx_solde_conge_employe_annee ON solde_conge (employe_id, annee);

CREATE TABLE demande_conge (
    id BIGSERIAL PRIMARY KEY,
    employe_id BIGINT NOT NULL REFERENCES employe(id),
    type_conge_id BIGINT NOT NULL REFERENCES type_conge(id),
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    demi_journee_debut BOOLEAN NOT NULL DEFAULT FALSE,
    demi_journee_fin BOOLEAN NOT NULL DEFAULT FALSE,
    nb_jours NUMERIC(7,2) NOT NULL,
    motif TEXT NOT NULL,
    statut VARCHAR(24) NOT NULL DEFAULT 'BROUILLON',
    valide_manager_id INT REFERENCES utilisateur(id),
    date_validation_manager TIMESTAMPTZ,
    valide_rh_id INT REFERENCES utilisateur(id),
    date_validation_rh TIMESTAMPTZ,
    motif_refus TEXT,
    date_soumission TIMESTAMPTZ,
    CONSTRAINT chk_demande_conge_dates CHECK (date_fin >= date_debut),
    CONSTRAINT chk_demande_conge_jours CHECK (nb_jours > 0),
    CONSTRAINT chk_demande_conge_statut CHECK (statut IN ('BROUILLON', 'SOUMISE', 'VALIDEE_MANAGER', 'VALIDEE_RH', 'REFUSEE', 'ANNULEE')),
    CONSTRAINT chk_demande_conge_validation_manager CHECK (
        statut NOT IN ('VALIDEE_MANAGER', 'VALIDEE_RH')
        OR (valide_manager_id IS NOT NULL AND date_validation_manager IS NOT NULL)
    ),
    CONSTRAINT chk_demande_conge_validation_rh CHECK (
        statut <> 'VALIDEE_RH'
        OR (valide_rh_id IS NOT NULL AND date_validation_rh IS NOT NULL)
    ),
    CONSTRAINT ex_demande_conge_periodes EXCLUDE USING gist (
        employe_id WITH =,
        daterange(date_debut, date_fin + 1, '[)') WITH &&
    ) WHERE (statut NOT IN ('REFUSEE', 'ANNULEE'))
);
CREATE INDEX idx_demande_conge_employe_statut ON demande_conge (employe_id, statut);
CREATE INDEX idx_demande_conge_dates ON demande_conge (date_debut, date_fin);

CREATE TABLE mouvement_solde (
    id BIGSERIAL PRIMARY KEY,
    solde_conge_id BIGINT NOT NULL REFERENCES solde_conge(id),
    type VARCHAR(20) NOT NULL,
    quantite NUMERIC(8,2) NOT NULL,
    demande_id BIGINT REFERENCES demande_conge(id),
    date DATE NOT NULL DEFAULT CURRENT_DATE,
    motif TEXT,
    cree_par_id INT REFERENCES utilisateur(id),
    CONSTRAINT chk_mouvement_solde_type CHECK (type IN ('ACQUISITION', 'PRISE', 'RESERVATION', 'LIBERATION', 'REPORT', 'AJUSTEMENT', 'EXPIRATION')),
    CONSTRAINT chk_mouvement_solde_quantite CHECK (quantite <> 0)
);
CREATE INDEX idx_mouvement_solde_solde_date ON mouvement_solde (solde_conge_id, date);
CREATE INDEX idx_mouvement_solde_demande ON mouvement_solde (demande_id);

CREATE TABLE absence (
    id BIGSERIAL PRIMARY KEY,
    employe_id BIGINT NOT NULL REFERENCES employe(id),
    type VARCHAR(24) NOT NULL,
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    statut VARCHAR(20) NOT NULL DEFAULT 'CONSTATEE',
    demande_conge_id BIGINT REFERENCES demande_conge(id),
    source VARCHAR(20) NOT NULL,
    commentaire TEXT,
    CONSTRAINT chk_absence_dates CHECK (date_fin >= date_debut),
    CONSTRAINT chk_absence_type CHECK (type IN ('MALADIE', 'INJUSTIFIEE', 'EVENEMENT_FAMILIAL', 'MISSION', 'AUTRE')),
    CONSTRAINT chk_absence_statut CHECK (statut IN ('CONSTATEE', 'JUSTIFIEE', 'NON_JUSTIFIEE')),
    CONSTRAINT chk_absence_source CHECK (source IN ('POINTAGE', 'SAISIE_RH', 'DEMANDE'))
);
CREATE INDEX idx_absence_employe_debut ON absence (employe_id, date_debut);

CREATE TABLE justificatif (
    id BIGSERIAL PRIMARY KEY,
    absence_id BIGINT REFERENCES absence(id),
    demande_conge_id BIGINT REFERENCES demande_conge(id),
    document_id BIGINT NOT NULL REFERENCES document(id),
    statut VARCHAR(16) NOT NULL DEFAULT 'DEPOSE',
    date_depot TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_justificatif_cible CHECK ((absence_id IS NULL) <> (demande_conge_id IS NULL)),
    CONSTRAINT chk_justificatif_statut CHECK (statut IN ('DEPOSE', 'VALIDE', 'REJETE'))
);
CREATE INDEX idx_justificatif_absence ON justificatif (absence_id);
CREATE INDEX idx_justificatif_demande ON justificatif (demande_conge_id);

CREATE TABLE delegation_validation (
    id BIGSERIAL PRIMARY KEY,
    manager_id BIGINT NOT NULL REFERENCES employe(id),
    delegue_id BIGINT NOT NULL REFERENCES employe(id),
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    CONSTRAINT chk_delegation_dates CHECK (date_fin >= date_debut),
    CONSTRAINT chk_delegation_distinct CHECK (manager_id <> delegue_id)
);
CREATE INDEX idx_delegation_manager_dates ON delegation_validation (manager_id, date_debut, date_fin);
