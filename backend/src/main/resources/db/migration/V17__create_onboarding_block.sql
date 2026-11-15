-- Bloc 4 : onboarding, checklist et inscription à la paie.
-- Les contrats du premier cycle sont créés par le bloc 3 ; cette migration
-- prépare uniquement le suivi post-embauche et les contrats suivants.

ALTER TABLE contrat
    ADD COLUMN motif VARCHAR(20);

ALTER TABLE contrat
    ADD CONSTRAINT chk_contrat_motif
        CHECK (motif IS NULL OR motif IN ('RENOUVELLEMENT', 'PASSAGE_CDI', 'AVENANT'));

CREATE TABLE onboarding (
    id BIGSERIAL PRIMARY KEY,
    employe_id BIGINT NOT NULL UNIQUE,
    statut VARCHAR(20) NOT NULL DEFAULT 'A_FAIRE',
    date_debut DATE NOT NULL,
    date_fin_prevue DATE,
    date_cloture DATE,
    responsable_id INT,

    CONSTRAINT fk_onboarding_employe
        FOREIGN KEY (employe_id) REFERENCES employe(id),
    CONSTRAINT fk_onboarding_responsable
        FOREIGN KEY (responsable_id) REFERENCES utilisateur(id),
    CONSTRAINT chk_onboarding_statut
        CHECK (statut IN ('A_FAIRE', 'EN_COURS', 'TERMINE', 'ANNULE')),
    CONSTRAINT chk_onboarding_dates
        CHECK (
            (date_fin_prevue IS NULL OR date_fin_prevue >= date_debut)
            AND (date_cloture IS NULL OR date_cloture >= date_debut)
        )
);

CREATE TABLE onboarding_modele (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(150) NOT NULL,
    poste_id INT,
    departement_id INT,
    actif BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_onboarding_modele_poste
        FOREIGN KEY (poste_id) REFERENCES poste(id),
    CONSTRAINT fk_onboarding_modele_departement
        FOREIGN KEY (departement_id) REFERENCES departement(id)
);

CREATE TABLE onboarding_modele_tache (
    id BIGSERIAL PRIMARY KEY,
    modele_id BIGINT NOT NULL,
    libelle VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL,
    responsable_role VARCHAR(50) NOT NULL,
    delai_jours INT NOT NULL DEFAULT 0,
    obligatoire BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_modele_tache_modele
        FOREIGN KEY (modele_id) REFERENCES onboarding_modele(id) ON DELETE CASCADE,
    CONSTRAINT chk_modele_tache_type
        CHECK (type IN ('CONTRAT', 'BADGE', 'COMPTE', 'MATERIEL', 'FORMATION', 'PAIE', 'AUTRE')),
    CONSTRAINT chk_modele_tache_delai
        CHECK (delai_jours >= 0)
);

CREATE TABLE onboarding_tache (
    id BIGSERIAL PRIMARY KEY,
    onboarding_id BIGINT NOT NULL,
    libelle VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL,
    statut VARCHAR(20) NOT NULL DEFAULT 'A_FAIRE',
    responsable_role VARCHAR(50) NOT NULL,
    assignee_id INT,
    date_echeance DATE NOT NULL,
    date_realisation DATE,
    obligatoire BOOLEAN NOT NULL DEFAULT TRUE,
    commentaire TEXT,

    CONSTRAINT fk_onboarding_tache_onboarding
        FOREIGN KEY (onboarding_id) REFERENCES onboarding(id) ON DELETE CASCADE,
    CONSTRAINT fk_onboarding_tache_assignee
        FOREIGN KEY (assignee_id) REFERENCES utilisateur(id),
    CONSTRAINT chk_onboarding_tache_type
        CHECK (type IN ('CONTRAT', 'BADGE', 'COMPTE', 'MATERIEL', 'FORMATION', 'PAIE', 'AUTRE')),
    CONSTRAINT chk_onboarding_tache_statut
        CHECK (statut IN ('A_FAIRE', 'EN_COURS', 'FAIT', 'NON_APPLICABLE')),
    CONSTRAINT chk_onboarding_tache_dates
        CHECK (date_realisation IS NULL OR date_realisation >= date_echeance)
);

CREATE UNIQUE INDEX uq_onboarding_tache_type
    ON onboarding_tache(onboarding_id, type)
    WHERE type <> 'AUTRE';

CREATE OR REPLACE FUNCTION verifier_echeance_onboarding()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    debut_onboarding DATE;
BEGIN
    SELECT date_debut INTO debut_onboarding
    FROM onboarding
    WHERE id = NEW.onboarding_id;

    IF debut_onboarding IS NULL OR NEW.date_echeance < debut_onboarding THEN
        RAISE EXCEPTION 'La date échéance doit être postérieure ou égale au début de onboarding';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_onboarding_tache_echeance
    BEFORE INSERT OR UPDATE OF onboarding_id, date_echeance
    ON onboarding_tache
    FOR EACH ROW
    EXECUTE FUNCTION verifier_echeance_onboarding();

CREATE TABLE document (
    id BIGSERIAL PRIMARY KEY,
    employe_id BIGINT NOT NULL,
    onboarding_tache_id BIGINT,
    type VARCHAR(30) NOT NULL,
    nom_fichier VARCHAR(255) NOT NULL,
    url VARCHAR(500) NOT NULL,
    taille BIGINT NOT NULL,
    date_depot TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    depose_par_id INT NOT NULL,

    CONSTRAINT fk_document_employe
        FOREIGN KEY (employe_id) REFERENCES employe(id),
    CONSTRAINT fk_document_tache
        FOREIGN KEY (onboarding_tache_id) REFERENCES onboarding_tache(id),
    CONSTRAINT fk_document_depose_par
        FOREIGN KEY (depose_par_id) REFERENCES utilisateur(id),
    CONSTRAINT chk_document_type
        CHECK (type IN ('CONTRAT', 'BADGE', 'COMPTE', 'MATERIEL', 'FORMATION', 'PAIE', 'AUTRE')),
    CONSTRAINT chk_document_taille
        CHECK (taille > 0)
);

CREATE TABLE inscription_paie (
    id BIGSERIAL PRIMARY KEY,
    employe_id BIGINT NOT NULL UNIQUE,
    date_inscription DATE,
    statut VARCHAR(20) NOT NULL DEFAULT 'A_FAIRE',
    reference_paie VARCHAR(100),
    mode_paiement VARCHAR(30),
    numero_compte VARCHAR(100),

    CONSTRAINT fk_inscription_paie_employe
        FOREIGN KEY (employe_id) REFERENCES employe(id),
    CONSTRAINT chk_inscription_paie_statut
        CHECK (statut IN ('A_FAIRE', 'INSCRIT', 'BLOQUE')),
    CONSTRAINT chk_inscription_paie_complete
        CHECK (
            statut <> 'INSCRIT'
            OR (
                date_inscription IS NOT NULL
                AND reference_paie IS NOT NULL
                AND mode_paiement IS NOT NULL
                AND numero_compte IS NOT NULL
            )
        )
);

CREATE INDEX idx_onboarding_tache_onboarding_statut
    ON onboarding_tache(onboarding_id, statut);

CREATE INDEX idx_onboarding_tache_assignee_statut
    ON onboarding_tache(assignee_id, statut);

CREATE INDEX idx_document_employe
    ON document(employe_id);

INSERT INTO onboarding_modele (nom, actif)
VALUES ('Checklist standard', TRUE);

INSERT INTO onboarding_modele_tache (
    modele_id, libelle, type, responsable_role, delai_jours, obligatoire
)
SELECT id, libelle, type, responsable_role, delai_jours, obligatoire
FROM onboarding_modele
CROSS JOIN (VALUES
    ('Contrat signé', 'CONTRAT', 'RH', 0, TRUE),
    ('Compte utilisateur', 'COMPTE', 'IT', 0, TRUE),
    ('Badge', 'BADGE', 'SERVICES_GENERAUX', 2, TRUE),
    ('Matériel', 'MATERIEL', 'IT', 3, TRUE),
    ('Formation d''intégration', 'FORMATION', 'MANAGER', 7, TRUE),
    ('Inscription paie', 'PAIE', 'RH', 5, TRUE)
) AS t(libelle, type, responsable_role, delai_jours, obligatoire)
WHERE onboarding_modele.nom = 'Checklist standard';

-- Les employés déjà présents reçoivent une checklist standard sans doublon.
INSERT INTO onboarding (employe_id, statut, date_debut, date_fin_prevue)
SELECT e.id,
       CASE (ROW_NUMBER() OVER (ORDER BY e.id) - 1) % 4
           WHEN 0 THEN 'A_FAIRE'
           WHEN 1 THEN 'EN_COURS'
           WHEN 2 THEN 'TERMINE'
           ELSE 'ANNULE'
       END,
       e.date_embauche, e.date_embauche + 30
FROM employe e
WHERE NOT EXISTS (
    SELECT 1 FROM onboarding o WHERE o.employe_id = e.id
);

INSERT INTO onboarding_tache (
    onboarding_id, libelle, type, responsable_role, date_echeance, obligatoire
)
SELECT o.id, m.libelle, m.type, m.responsable_role,
       o.date_debut + m.delai_jours, m.obligatoire
FROM onboarding o
JOIN onboarding_modele_tache m
  ON m.modele_id = (SELECT id FROM onboarding_modele WHERE nom = 'Checklist standard' LIMIT 1)
WHERE NOT EXISTS (
    SELECT 1 FROM onboarding_tache t
    WHERE t.onboarding_id = o.id AND t.type = m.type
);
