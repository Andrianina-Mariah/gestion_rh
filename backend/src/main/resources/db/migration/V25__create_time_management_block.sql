-- Bloc 6 : régimes de travail, planning, pointage et variables de paie.
-- Cette migration stocke des quantités de temps ; elle ne calcule aucun salaire.
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE regime_travail (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    libelle VARCHAR(150) NOT NULL,
    heures_hebdo NUMERIC(5, 2) NOT NULL,
    jours_travailles_par_semaine NUMERIC(3, 1) NOT NULL,
    tolerance_retard_min INT NOT NULL DEFAULT 0,
    CONSTRAINT chk_regime_heures_hebdo CHECK (heures_hebdo > 0 AND heures_hebdo <= 168),
    CONSTRAINT chk_regime_jours_semaine CHECK (jours_travailles_par_semaine > 0 AND jours_travailles_par_semaine <= 7),
    CONSTRAINT chk_regime_tolerance CHECK (tolerance_retard_min >= 0)
);

CREATE TABLE horaire_type (
    id BIGSERIAL PRIMARY KEY,
    regime_travail_id BIGINT NOT NULL,
    jour_semaine SMALLINT NOT NULL,
    heure_debut TIME NOT NULL,
    heure_fin TIME NOT NULL,
    pause_minutes INT NOT NULL DEFAULT 0,
    travaille BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_horaire_type_regime FOREIGN KEY (regime_travail_id) REFERENCES regime_travail(id),
    CONSTRAINT chk_horaire_type_jour CHECK (jour_semaine BETWEEN 1 AND 7),
    CONSTRAINT chk_horaire_type_pause CHECK (pause_minutes >= 0),
    CONSTRAINT chk_horaire_type_heures CHECK (NOT travaille OR heure_fin > heure_debut),
    CONSTRAINT uq_horaire_type_jour UNIQUE (regime_travail_id, jour_semaine)
);

CREATE TABLE employe_regime (
    id BIGSERIAL PRIMARY KEY,
    employe_id BIGINT NOT NULL REFERENCES employe(id),
    regime_travail_id BIGINT NOT NULL REFERENCES regime_travail(id),
    date_debut DATE NOT NULL,
    date_fin DATE,
    CONSTRAINT chk_employe_regime_dates CHECK (date_fin IS NULL OR date_fin >= date_debut),
    CONSTRAINT ex_employe_regime_periodes EXCLUDE USING gist (
        employe_id WITH =,
        daterange(date_debut, COALESCE(date_fin + 1, 'infinity'::date), '[)') WITH &&
    )
);
CREATE INDEX idx_employe_regime_employe_dates ON employe_regime(employe_id, date_debut, date_fin);

CREATE TABLE jour_ferie (
    id BIGSERIAL PRIMARY KEY,
    date DATE NOT NULL UNIQUE,
    libelle VARCHAR(150) NOT NULL,
    paye BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE planning (
    id BIGSERIAL PRIMARY KEY,
    employe_id BIGINT NOT NULL REFERENCES employe(id),
    date DATE NOT NULL,
    heure_debut TIME NOT NULL,
    heure_fin TIME NOT NULL,
    type VARCHAR(20) NOT NULL,
    source VARCHAR(20) NOT NULL,
    CONSTRAINT chk_planning_type CHECK (type IN ('TRAVAIL', 'REPOS', 'CONGE', 'ABSENCE', 'FERIE')),
    CONSTRAINT chk_planning_source CHECK (source IN ('REGIME', 'MANUEL', 'CONGE')),
    CONSTRAINT chk_planning_heures CHECK (heure_fin > heure_debut),
    CONSTRAINT uq_planning_employe_date_debut UNIQUE (employe_id, date, heure_debut)
);
CREATE INDEX idx_planning_employe_date ON planning(employe_id, date);

CREATE TABLE pointage (
    id BIGSERIAL PRIMARY KEY,
    employe_id BIGINT NOT NULL REFERENCES employe(id),
    date_heure TIMESTAMPTZ NOT NULL,
    sens VARCHAR(10) NOT NULL,
    source VARCHAR(10) NOT NULL,
    cree_par_id INT REFERENCES utilisateur(id),
    valide_par_id INT REFERENCES utilisateur(id),
    date_validation TIMESTAMPTZ,
    motif_correction TEXT,
    pointage_origine_id BIGINT REFERENCES pointage(id),
    CONSTRAINT chk_pointage_sens CHECK (sens IN ('ENTREE', 'SORTIE')),
    CONSTRAINT chk_pointage_source CHECK (source IN ('WEB', 'IMPORT', 'MANUEL')),
    CONSTRAINT chk_pointage_motif CHECK (
        (source <> 'MANUEL' AND pointage_origine_id IS NULL AND motif_correction IS NULL)
        OR (length(btrim(motif_correction)) > 0)
    ),
    CONSTRAINT chk_pointage_validation CHECK (
        (source <> 'MANUEL' AND pointage_origine_id IS NULL AND valide_par_id IS NULL AND date_validation IS NULL)
        OR (valide_par_id IS NOT NULL AND date_validation IS NOT NULL)
    )
);
CREATE INDEX idx_pointage_employe_date_heure ON pointage(employe_id, date_heure);
CREATE INDEX idx_pointage_origine ON pointage(pointage_origine_id) WHERE pointage_origine_id IS NOT NULL;

CREATE TABLE journee_travail (
    id BIGSERIAL PRIMARY KEY,
    employe_id BIGINT NOT NULL REFERENCES employe(id),
    date DATE NOT NULL,
    heure_arrivee TIMESTAMPTZ,
    heure_depart TIMESTAMPTZ,
    minutes_travaillees INT NOT NULL DEFAULT 0,
    minutes_retard INT NOT NULL DEFAULT 0,
    minutes_depart_anticipe INT NOT NULL DEFAULT 0,
    minutes_sup INT NOT NULL DEFAULT 0,
    statut VARCHAR(20) NOT NULL DEFAULT 'OUVERTE',
    CONSTRAINT uq_journee_travail_employe_date UNIQUE (employe_id, date),
    CONSTRAINT chk_journee_travail_statut CHECK (statut IN ('OUVERTE', 'CLOTUREE', 'VALIDEE', 'EN_ANOMALIE')),
    CONSTRAINT chk_journee_travail_minutes CHECK (minutes_travaillees >= 0 AND minutes_retard >= 0 AND minutes_depart_anticipe >= 0 AND minutes_sup >= 0),
    CONSTRAINT chk_journee_travail_dates CHECK (heure_depart IS NULL OR heure_arrivee IS NULL OR heure_depart >= heure_arrivee)
);
CREATE INDEX idx_journee_travail_employe_date ON journee_travail(employe_id, date);

CREATE TABLE heure_supplementaire (
    id BIGSERIAL PRIMARY KEY,
    employe_id BIGINT NOT NULL REFERENCES employe(id),
    date DATE NOT NULL,
    minutes INT NOT NULL,
    taux NUMERIC(5, 2) NOT NULL,
    statut VARCHAR(20) NOT NULL DEFAULT 'DEMANDEE',
    valide_par_id INT REFERENCES utilisateur(id),
    motif TEXT,
    CONSTRAINT chk_heure_supplementaire_minutes CHECK (minutes > 0),
    CONSTRAINT chk_heure_supplementaire_taux CHECK (taux >= 0),
    CONSTRAINT chk_heure_supplementaire_statut CHECK (statut IN ('DEMANDEE', 'VALIDEE', 'REFUSEE'))
);
CREATE INDEX idx_heure_supplementaire_employe_date ON heure_supplementaire(employe_id, date);

CREATE TABLE anomalie_pointage (
    id BIGSERIAL PRIMARY KEY,
    employe_id BIGINT NOT NULL REFERENCES employe(id),
    date DATE NOT NULL,
    type VARCHAR(30) NOT NULL,
    statut VARCHAR(20) NOT NULL DEFAULT 'OUVERTE',
    commentaire TEXT,
    CONSTRAINT chk_anomalie_pointage_type CHECK (type IN ('ENTREE_MANQUANTE', 'SORTIE_MANQUANTE', 'DOUBLON', 'HORS_PLANNING', 'DUREE_EXCESSIVE', 'RETARD_NON_JUSTIFIE')),
    CONSTRAINT chk_anomalie_pointage_statut CHECK (statut IN ('OUVERTE', 'JUSTIFIEE', 'CORRIGEE', 'IGNOREE'))
);
CREATE INDEX idx_anomalie_pointage_statut_date ON anomalie_pointage(statut, date);
CREATE INDEX idx_anomalie_pointage_employe_date ON anomalie_pointage(employe_id, date);

CREATE TABLE periode_temps (
    id BIGSERIAL PRIMARY KEY,
    annee SMALLINT NOT NULL,
    mois SMALLINT NOT NULL,
    statut VARCHAR(20) NOT NULL DEFAULT 'OUVERTE',
    CONSTRAINT uq_periode_temps_annee_mois UNIQUE (annee, mois),
    CONSTRAINT chk_periode_temps_mois CHECK (mois BETWEEN 1 AND 12),
    CONSTRAINT chk_periode_temps_annee CHECK (annee BETWEEN 1900 AND 9999),
    CONSTRAINT chk_periode_temps_statut CHECK (statut IN ('OUVERTE', 'VALIDEE', 'EXPORTEE'))
);

CREATE TABLE variable_paie (
    id BIGSERIAL PRIMARY KEY,
    employe_id BIGINT NOT NULL REFERENCES employe(id),
    periode_id BIGINT NOT NULL REFERENCES periode_temps(id),
    type VARCHAR(30) NOT NULL,
    quantite_minutes INT NOT NULL,
    source VARCHAR(100) NOT NULL,
    CONSTRAINT chk_variable_paie_type CHECK (type IN ('HEURES_NORMALES', 'HEURES_SUP_30', 'HEURES_SUP_50', 'NUIT', 'DIMANCHE_FERIE', 'RETARD', 'ABSENCE')),
    CONSTRAINT chk_variable_paie_quantite CHECK (quantite_minutes >= 0)
);
CREATE INDEX idx_variable_paie_periode_employe ON variable_paie(periode_id, employe_id);
