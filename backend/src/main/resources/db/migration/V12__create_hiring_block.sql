-- Bloc 3 : proposition d'embauche, salarié, contrats et affectations.
-- La checklist onboarding reste hors périmètre et sera créée au Bloc 4.

CREATE SEQUENCE employe_matricule_seq START WITH 1 INCREMENT BY 1;

CREATE OR REPLACE FUNCTION generer_matricule_employe()
RETURNS VARCHAR(50)
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN 'EMP-' || EXTRACT(YEAR FROM CURRENT_DATE)::INT
        || '-' || LPAD(nextval('employe_matricule_seq')::TEXT, 4, '0');
END;
$$;

CREATE TABLE proposition_embauche (
    id BIGSERIAL PRIMARY KEY,
    candidature_id BIGINT NOT NULL,
    version INT NOT NULL DEFAULT 1,
    proposition_precedente_id BIGINT,
    emise_par_id BIGINT NOT NULL,
    date_travail_souhaitee DATE NOT NULL,
    lieu_travail VARCHAR(150) NOT NULL,
    salaire_propose NUMERIC(12, 2) NOT NULL,
    type_contrat_propose VARCHAR(3) NOT NULL,
    statut VARCHAR(50) NOT NULL DEFAULT 'EN_ATTENTE',
    remarques_candidat TEXT,
    date_proposition TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_reponse TIMESTAMP,

    CONSTRAINT fk_proposition_candidature
        FOREIGN KEY (candidature_id) REFERENCES candidature(id),
    CONSTRAINT fk_proposition_precedente
        FOREIGN KEY (proposition_precedente_id) REFERENCES proposition_embauche(id),
    CONSTRAINT fk_proposition_emise_par
        FOREIGN KEY (emise_par_id) REFERENCES utilisateur(id),
    CONSTRAINT chk_proposition_version
        CHECK (version > 0),
    CONSTRAINT chk_proposition_statut
        CHECK (statut IN ('EN_ATTENTE', 'ACCEPTE', 'REFUSE', 'NEGOCIATION')),
    CONSTRAINT chk_proposition_type_contrat
        CHECK (type_contrat_propose IN ('CE', 'CDD', 'CDI')),
    CONSTRAINT chk_proposition_salaire
        CHECK (salaire_propose > 0),
    CONSTRAINT chk_proposition_date_reponse
        CHECK (date_reponse IS NULL OR date_reponse >= date_proposition)
);

CREATE UNIQUE INDEX uq_proposition_candidature_version
    ON proposition_embauche(candidature_id, version);

CREATE UNIQUE INDEX uq_proposition_active_candidature
    ON proposition_embauche(candidature_id)
    WHERE statut IN ('EN_ATTENTE', 'NEGOCIATION');

CREATE INDEX idx_proposition_candidature
    ON proposition_embauche(candidature_id);

CREATE TABLE employe (
    id BIGSERIAL PRIMARY KEY,
    candidat_id BIGINT NOT NULL,
    matricule VARCHAR(50) NOT NULL UNIQUE DEFAULT generer_matricule_employe(),
    email_professionnel VARCHAR(150),
    telephone_professionnel VARCHAR(30),
    date_embauche DATE NOT NULL,
    statut VARCHAR(50) NOT NULL DEFAULT 'ACTIF',

    CONSTRAINT fk_employe_candidat
        FOREIGN KEY (candidat_id) REFERENCES candidat(id),
    CONSTRAINT uq_employe_candidat
        UNIQUE (candidat_id),
    CONSTRAINT chk_employe_statut
        CHECK (statut IN ('ACTIF', 'EN_ONBOARDING', 'SORTIE'))
);

CREATE INDEX idx_employe_statut ON employe(statut);

CREATE TABLE contrat (
    id BIGSERIAL PRIMARY KEY,
    employe_id BIGINT NOT NULL,
    type_contrat VARCHAR(3) NOT NULL,
    date_debut DATE NOT NULL,
    date_fin DATE,
    salaire NUMERIC(12, 2) NOT NULL,
    statut VARCHAR(20) NOT NULL DEFAULT 'BROUILLON',
    contrat_url VARCHAR(255),
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    numero_renouvellement INT NOT NULL DEFAULT 0,
    contrat_precedent_id BIGINT,

    CONSTRAINT fk_contrat_employe
        FOREIGN KEY (employe_id) REFERENCES employe(id),
    CONSTRAINT fk_contrat_precedent
        FOREIGN KEY (contrat_precedent_id) REFERENCES contrat(id),
    CONSTRAINT chk_contrat_type
        CHECK (type_contrat IN ('CE', 'CDD', 'CDI')),
    CONSTRAINT chk_contrat_dates
        CHECK (
            (type_contrat = 'CDI' AND date_fin IS NULL)
            OR (type_contrat IN ('CE', 'CDD') AND date_fin IS NOT NULL AND date_fin > date_debut)
        ),
    CONSTRAINT chk_contrat_salaire
        CHECK (salaire > 0),
    CONSTRAINT chk_contrat_statut
        CHECK (statut IN ('BROUILLON', 'CREE', 'SIGNE', 'VALIDE')),
    CONSTRAINT chk_contrat_renouvellement
        CHECK (numero_renouvellement >= 0)
);

CREATE INDEX idx_contrat_employe_statut
    ON contrat(employe_id, statut);

CREATE TABLE affectation (
    id BIGSERIAL PRIMARY KEY,
    employe_id BIGINT NOT NULL,
    poste_id INT NOT NULL,
    departement_id INT NOT NULL,
    date_affectation DATE NOT NULL,
    date_fin DATE,

    CONSTRAINT fk_affectation_employe
        FOREIGN KEY (employe_id) REFERENCES employe(id),
    CONSTRAINT fk_affectation_poste
        FOREIGN KEY (poste_id) REFERENCES poste(id),
    CONSTRAINT fk_affectation_departement
        FOREIGN KEY (departement_id) REFERENCES departement(id),
    CONSTRAINT chk_affectation_dates
        CHECK (date_fin IS NULL OR date_fin >= date_affectation)
);

CREATE INDEX idx_affectation_employe_dates
    ON affectation(employe_id, date_fin);

-- Données de démonstration : une proposition dans chaque état du workflow.
WITH candidats AS (
    SELECT c.id AS candidat_id, ca.id AS candidature_id,
           ROW_NUMBER() OVER (ORDER BY c.id) AS numero
    FROM candidat c
    JOIN candidature ca ON ca.candidat_id = c.id
    WHERE c.email LIKE '%@example.com'
    ORDER BY c.id
    LIMIT 4
),
rh AS (
    SELECT id FROM utilisateur WHERE email = 'rh@sirh.mg'
)
INSERT INTO proposition_embauche (
    candidature_id,
    version,
    emise_par_id,
    date_travail_souhaitee,
    lieu_travail,
    salaire_propose,
    type_contrat_propose,
    statut,
    remarques_candidat,
    date_reponse
)
SELECT
    c.candidature_id,
    1,
    rh.id,
    CURRENT_DATE + 30,
    'Antananarivo',
    2500000 + (c.numero * 100000),
    CASE c.numero WHEN 1 THEN 'CE' WHEN 2 THEN 'CDD' ELSE 'CDI' END,
    CASE c.numero
        WHEN 1 THEN 'EN_ATTENTE'
        WHEN 2 THEN 'NEGOCIATION'
        WHEN 3 THEN 'ACCEPTE'
        ELSE 'REFUSE'
    END,
    CASE c.numero
        WHEN 2 THEN 'Le candidat souhaite négocier le type de contrat.'
        WHEN 4 THEN 'Conditions refusées par le candidat.'
        ELSE NULL
    END,
    CASE WHEN c.numero IN (2, 3, 4) THEN CURRENT_TIMESTAMP ELSE NULL END
FROM candidats c
CROSS JOIN rh;
