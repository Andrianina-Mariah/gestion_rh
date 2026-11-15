-- Partie 2 : circuit de validation, traçabilité et ouverture du recrutement.

ALTER TABLE demande_recrutement
    ADD COLUMN cycle INT NOT NULL DEFAULT 1,
    ADD COLUMN version INT NOT NULL DEFAULT 0,
    ADD COLUMN date_ouverture_recrutement TIMESTAMP,
    ADD COLUMN ouvert_par_id INT;

ALTER TABLE demande_recrutement
    ADD CONSTRAINT fk_demande_ouvert_par
        FOREIGN KEY (ouvert_par_id) REFERENCES utilisateur(id),
    ADD CONSTRAINT chk_demande_cycle
        CHECK (cycle > 0),
    ADD CONSTRAINT chk_demande_version
        CHECK (version >= 0),
    ADD CONSTRAINT chk_demande_statut_partie_2
        CHECK (statut IN (
            'BROUILLON',
            'EN_ATTENTE_RH',
            'ANALYSE_FINANCE',
            'EN_ATTENTE_DIRECTION',
            'A_CORRIGER',
            'REJETE',
            'VALIDE',
            'RECRUTEMENT_OUVERT'
        ));

ALTER TABLE validation_demande
    ADD COLUMN budget_valide NUMERIC(12, 2),
    ADD COLUMN cycle INT NOT NULL DEFAULT 1;

ALTER TABLE validation_demande
    ADD CONSTRAINT chk_validation_etape
        CHECK (etape IN ('RH', 'FINANCE', 'DIRECTION')),
    ADD CONSTRAINT chk_validation_statut
        CHECK (statut IN ('ACCEPTE', 'REJETE', 'DEMANDE_ANALYSE', 'A_CORRIGER')),
    ADD CONSTRAINT chk_validation_budget_valide
        CHECK (budget_valide IS NULL OR budget_valide > 0),
    ADD CONSTRAINT chk_validation_cycle
        CHECK (cycle > 0);

CREATE INDEX idx_validation_demande_cycle
    ON validation_demande (demande_recrutement_id, cycle);

CREATE INDEX idx_demande_recrutement_statut
    ON demande_recrutement (statut);

CREATE TABLE notification (
    id SERIAL PRIMARY KEY,
    destinataire_id INT NOT NULL,
    message TEXT NOT NULL,
    lien VARCHAR(255),
    lue BOOLEAN NOT NULL DEFAULT FALSE,
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_notification_destinataire
        FOREIGN KEY (destinataire_id) REFERENCES utilisateur(id)
);

CREATE INDEX idx_notification_destinataire_lue
    ON notification (destinataire_id, lue);

CREATE INDEX idx_notification_date_creation
    ON notification (date_creation);

CREATE OR REPLACE FUNCTION refuser_modification_validation_demande()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'validation_demande est une table append-only : modification et suppression interdites';
END;
$$;

CREATE TRIGGER trg_validation_demande_append_only
BEFORE UPDATE OR DELETE ON validation_demande
FOR EACH ROW
EXECUTE FUNCTION refuser_modification_validation_demande();

-- Jeu de données de démonstration : une demande dans chaque état du workflow.
INSERT INTO demande_recrutement (
    poste_id,
    demandeur_id,
    nombre_postes,
    type,
    motif,
    budget,
    date_souhaitee,
    statut,
    cycle,
    version,
    date_ouverture_recrutement,
    ouvert_par_id
)
SELECT
    p.id,
    manager.id,
    1,
    'CREATION',
    demo.motif,
    demo.budget,
    CURRENT_DATE + demo.jours,
    demo.statut,
    demo.cycle,
    demo.version,
    CASE WHEN demo.statut = 'RECRUTEMENT_OUVERT' THEN CURRENT_TIMESTAMP ELSE NULL END,
    CASE WHEN demo.statut = 'RECRUTEMENT_OUVERT' THEN rh.id ELSE NULL END
FROM (
    VALUES
        ('BROUILLON', 1, 0, 30, 1000.00, 'Demande de démonstration en brouillon'),
        ('EN_ATTENTE_RH', 1, 0, 31, 1100.00, 'Demande de démonstration en attente RH'),
        ('ANALYSE_FINANCE', 1, 1, 32, 1200.00, 'Demande de démonstration en analyse financière'),
        ('EN_ATTENTE_DIRECTION', 1, 2, 33, 1300.00, 'Demande de démonstration en attente Direction'),
        ('A_CORRIGER', 2, 3, 34, 1400.00, 'Demande de démonstration à corriger'),
        ('REJETE', 1, 1, 35, 1500.00, 'Demande de démonstration rejetée'),
        ('VALIDE', 1, 2, 36, 1600.00, 'Demande de démonstration validée'),
        ('RECRUTEMENT_OUVERT', 1, 3, 37, 1700.00, 'Demande de démonstration ouverte au recrutement')
) AS demo(statut, cycle, version, jours, budget, motif)
CROSS JOIN LATERAL (
    SELECT id
    FROM poste
    ORDER BY id
    LIMIT 1
) p
JOIN utilisateur manager ON manager.email = 'manager@sirh.mg'
JOIN utilisateur rh ON rh.email = 'rh@sirh.mg';
