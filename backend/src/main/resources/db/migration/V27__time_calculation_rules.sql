-- Règles configurables et agrégats journaliers utilisés par le moteur de temps.
ALTER TABLE horaire_type DROP CONSTRAINT chk_horaire_type_heures;
ALTER TABLE horaire_type ADD CONSTRAINT chk_horaire_type_heures CHECK (NOT travaille OR heure_fin <> heure_debut);
ALTER TABLE planning DROP CONSTRAINT chk_planning_heures;
ALTER TABLE planning ADD CONSTRAINT chk_planning_heures CHECK (heure_fin <> heure_debut);
ALTER TABLE planning ADD COLUMN pause_minutes INT NOT NULL DEFAULT 0 CHECK (pause_minutes >= 0);

ALTER TABLE journee_travail
    ADD COLUMN minutes_normales INT NOT NULL DEFAULT 0 CHECK (minutes_normales >= 0),
    ADD COLUMN minutes_nuit INT NOT NULL DEFAULT 0 CHECK (minutes_nuit >= 0),
    ADD COLUMN minutes_dimanche_ferie INT NOT NULL DEFAULT 0 CHECK (minutes_dimanche_ferie >= 0),
    ADD COLUMN minutes_absence INT NOT NULL DEFAULT 0 CHECK (minutes_absence >= 0),
    ADD COLUMN minutes_sup_validees INT NOT NULL DEFAULT 0 CHECK (minutes_sup_validees >= 0);

CREATE TABLE regle_temps (
    id SMALLINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    nuit_debut TIME NOT NULL DEFAULT TIME '21:00',
    nuit_fin TIME NOT NULL DEFAULT TIME '05:00',
    taux_sup_30 NUMERIC(5,2) NOT NULL DEFAULT 30 CHECK (taux_sup_30 >= 0),
    taux_sup_50 NUMERIC(5,2) NOT NULL DEFAULT 50 CHECK (taux_sup_50 >= 0),
    seuil_sup_50_minutes INT NOT NULL DEFAULT 480 CHECK (seuil_sup_50_minutes >= 0),
    quota_sup_hebdo_minutes INT NOT NULL DEFAULT 1200 CHECK (quota_sup_hebdo_minutes > 0),
    quota_sup_annuel_minutes INT NOT NULL DEFAULT 72000 CHECK (quota_sup_annuel_minutes > 0),
    duree_max_journaliere_minutes INT NOT NULL DEFAULT 720 CHECK (duree_max_journaliere_minutes > 0)
);
INSERT INTO regle_temps(id) VALUES (1);

CREATE INDEX idx_heure_sup_employe_statut_date ON heure_supplementaire(employe_id, statut, date);
