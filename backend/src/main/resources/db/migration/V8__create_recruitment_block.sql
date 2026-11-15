-- Bloc 2 : offres d'emploi, candidatures et évaluations.

CREATE TABLE offre_emploi (
    id BIGSERIAL PRIMARY KEY,
    demande_recrutement_id INT NOT NULL,
    titre_offre VARCHAR(200) NOT NULL,
    description_poste TEXT NOT NULL,
    profil_recherche TEXT NOT NULL,
    lieu_travail VARCHAR(100),
    type_contrat_propose VARCHAR(50),
    statut VARCHAR(50) NOT NULL DEFAULT 'BROUILLON',
    date_publication TIMESTAMP,
    date_cloture TIMESTAMP,

    CONSTRAINT fk_offre_demande
        FOREIGN KEY (demande_recrutement_id) REFERENCES demande_recrutement(id),
    CONSTRAINT chk_offre_statut
        CHECK (statut IN ('BROUILLON', 'PUBLIEE', 'CLOTUREE')),
    CONSTRAINT chk_offre_dates
        CHECK (date_cloture IS NULL OR date_publication IS NULL OR date_cloture >= date_publication)
);

CREATE INDEX idx_offre_statut ON offre_emploi(statut);
CREATE INDEX idx_offre_demande ON offre_emploi(demande_recrutement_id);

CREATE TABLE candidat (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    telephone VARCHAR(30),
    adresse TEXT,
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE cv_candidature (
    id BIGSERIAL PRIMARY KEY,
    candidat_id INT NOT NULL,
    titre_profil VARCHAR(150),
    resume TEXT,
    competences TEXT,
    langues VARCHAR(100),
    date_mise_a_jour TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_cv_candidat
        FOREIGN KEY (candidat_id) REFERENCES candidat(id)
);

CREATE INDEX idx_cv_candidat ON cv_candidature(candidat_id);

CREATE TABLE experience_professionnelle (
    id BIGSERIAL PRIMARY KEY,
    cv_candidature_id INT NOT NULL,
    poste VARCHAR(150) NOT NULL,
    entreprise VARCHAR(150) NOT NULL,
    date_debut DATE,
    date_fin DATE,
    en_poste BOOLEAN NOT NULL DEFAULT FALSE,
    description TEXT,

    CONSTRAINT fk_experience_cv
        FOREIGN KEY (cv_candidature_id) REFERENCES cv_candidature(id) ON DELETE CASCADE,
    CONSTRAINT chk_experience_dates
        CHECK (date_fin IS NULL OR date_debut IS NULL OR date_fin >= date_debut)
);

CREATE TABLE formation_candidat (
    id BIGSERIAL PRIMARY KEY,
    cv_candidature_id INT NOT NULL,
    diplome VARCHAR(150) NOT NULL,
    etablissement VARCHAR(150) NOT NULL,
    annee_obtention INT,
    description TEXT,

    CONSTRAINT fk_formation_cv
        FOREIGN KEY (cv_candidature_id) REFERENCES cv_candidature(id) ON DELETE CASCADE
);

CREATE TABLE candidature (
    id BIGSERIAL PRIMARY KEY,
    offre_emploi_id INT NOT NULL,
    candidat_id INT NOT NULL,
    cv_candidature_id INT,
    cv_url VARCHAR(255),
    lettre_motivation_url VARCHAR(255),
    cv_snapshot JSONB NOT NULL,
    statut VARCHAR(50) NOT NULL DEFAULT 'RECU',
    rang_attente INT,
    date_candidature TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_candidature_offre
        FOREIGN KEY (offre_emploi_id) REFERENCES offre_emploi(id),
    CONSTRAINT fk_candidature_candidat
        FOREIGN KEY (candidat_id) REFERENCES candidat(id),
    CONSTRAINT fk_candidature_cv
        FOREIGN KEY (cv_candidature_id) REFERENCES cv_candidature(id),
    CONSTRAINT uq_candidature_offre_candidat
        UNIQUE (offre_emploi_id, candidat_id),
    CONSTRAINT chk_candidature_statut
        CHECK (statut IN (
            'RECU',
            'PRESELECTIONNE',
            'EN_ATTENTE',
            'REJETE',
            'TEST_PLANIFIE',
            'TEST_PASSE',
            'ENTRETIEN_PLANIFIE',
            'ENTRETIEN_PASSE',
            'RETENU'
        )),
    CONSTRAINT chk_candidature_rang_attente
        CHECK (rang_attente IS NULL OR rang_attente > 0)
);

CREATE INDEX idx_candidature_offre_statut
    ON candidature(offre_emploi_id, statut);
CREATE INDEX idx_candidature_candidat
    ON candidature(candidat_id);

CREATE TABLE candidature_historique (
    id BIGSERIAL PRIMARY KEY,
    candidature_id INT NOT NULL,
    ancien_statut VARCHAR(50),
    nouveau_statut VARCHAR(50) NOT NULL,
    utilisateur_id INT,
    commentaire TEXT,
    date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_candidature_historique_candidature
        FOREIGN KEY (candidature_id) REFERENCES candidature(id),
    CONSTRAINT fk_candidature_historique_utilisateur
        FOREIGN KEY (utilisateur_id) REFERENCES utilisateur(id),
    CONSTRAINT chk_historique_ancien_statut
        CHECK (ancien_statut IS NULL OR ancien_statut IN (
            'RECU',
            'PRESELECTIONNE',
            'EN_ATTENTE',
            'REJETE',
            'TEST_PLANIFIE',
            'TEST_PASSE',
            'ENTRETIEN_PLANIFIE',
            'ENTRETIEN_PASSE',
            'RETENU'
        )),
    CONSTRAINT chk_historique_nouveau_statut
        CHECK (nouveau_statut IN (
            'RECU',
            'PRESELECTIONNE',
            'EN_ATTENTE',
            'REJETE',
            'TEST_PLANIFIE',
            'TEST_PASSE',
            'ENTRETIEN_PLANIFIE',
            'ENTRETIEN_PASSE',
            'RETENU'
        ))
);

CREATE INDEX idx_candidature_historique_date
    ON candidature_historique(candidature_id, date);

CREATE TABLE evaluation (
    id BIGSERIAL PRIMARY KEY,
    candidature_id INT NOT NULL,
    type_evaluation VARCHAR(20) NOT NULL,
    date_planifiee TIMESTAMP NOT NULL,
    date_realisation TIMESTAMP,
    note NUMERIC(5, 2),
    remarque TEXT,
    resultat VARCHAR(30),
    evaluateur_id INT,

    CONSTRAINT fk_evaluation_candidature
        FOREIGN KEY (candidature_id) REFERENCES candidature(id),
    CONSTRAINT fk_evaluation_evaluateur
        FOREIGN KEY (evaluateur_id) REFERENCES utilisateur(id),
    CONSTRAINT chk_evaluation_type
        CHECK (type_evaluation IN ('TEST', 'ENTRETIEN')),
    CONSTRAINT chk_evaluation_note
        CHECK (note IS NULL OR note >= 0 AND note <= 20),
    CONSTRAINT chk_evaluation_resultat
        CHECK (resultat IS NULL OR resultat IN ('ACCEPTE', 'LISTE_ATTENTE', 'REJETE')),
    CONSTRAINT chk_evaluation_dates
        CHECK (date_realisation IS NULL OR date_realisation >= date_planifiee)
);

CREATE INDEX idx_evaluation_candidature_type
    ON evaluation(candidature_id, type_evaluation);

CREATE OR REPLACE FUNCTION refuser_modification_candidature_historique()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'candidature_historique est une table append-only : modification et suppression interdites';
END;
$$;

CREATE TRIGGER trg_candidature_historique_append_only
BEFORE UPDATE OR DELETE ON candidature_historique
FOR EACH ROW
EXECUTE FUNCTION refuser_modification_candidature_historique();

-- Jeu de données de démonstration : une offre publiée et six candidatures
-- couvrant les principales étapes du workflow.
INSERT INTO offre_emploi (
    demande_recrutement_id,
    titre_offre,
    description_poste,
    profil_recherche,
    lieu_travail,
    type_contrat_propose,
    statut,
    date_publication
)
SELECT
    d.id,
    p.titre,
    'Développement et maintenance des applications du SIRH.',
    'Java 21, Spring Boot, PostgreSQL et bonnes pratiques de développement.',
    'Antananarivo',
    'CDI',
    'PUBLIEE',
    CURRENT_TIMESTAMP
FROM demande_recrutement d
JOIN poste p ON p.id = d.poste_id
WHERE d.statut = 'RECRUTEMENT_OUVERT'
ORDER BY d.id
LIMIT 1;

INSERT INTO candidat (nom, prenom, email, telephone, adresse) VALUES
    ('Rakoto', 'Aina', 'aina.rakoto@example.com', '+261340000001', 'Antananarivo'),
    ('Rasoanaivo', 'Bema', 'bema.rasoanaivo@example.com', '+261340000002', 'Antananarivo'),
    ('Andriamihaja', 'Lova', 'lova.andriamihaja@example.com', '+261340000003', 'Fianarantsoa'),
    ('Randrianasolo', 'Mamy', 'mamy.randrianasolo@example.com', '+261340000004', 'Toamasina'),
    ('Ravelomanana', 'Soa', 'soa.ravelomanana@example.com', '+261340000005', 'Antsirabe'),
    ('Razafindrakoto', 'Hery', 'hery.razafindrakoto@example.com', '+261340000006', 'Mahajanga');

INSERT INTO cv_candidature (candidat_id, titre_profil, resume, competences, langues)
SELECT id, 'Développeur Java', 'Profil développeur Java avec expérience backend.',
       'Java, Spring Boot, PostgreSQL', 'Français, Anglais'
FROM candidat
WHERE email IN (
    'aina.rakoto@example.com',
    'bema.rasoanaivo@example.com',
    'lova.andriamihaja@example.com',
    'mamy.randrianasolo@example.com',
    'soa.ravelomanana@example.com',
    'hery.razafindrakoto@example.com'
);

WITH offre AS (
    SELECT id FROM offre_emploi ORDER BY id DESC LIMIT 1
)
INSERT INTO candidature (
    offre_emploi_id,
    candidat_id,
    cv_candidature_id,
    cv_snapshot,
    statut,
    rang_attente
)
SELECT
    offre.id,
    c.id,
    cv.id,
    jsonb_build_object(
        'profil', cv.titre_profil,
        'resume', cv.resume,
        'competences', cv.competences,
        'langues', cv.langues
    ),
    donnees.statut,
    donnees.rang_attente
FROM offre
JOIN (
    VALUES
        ('aina.rakoto@example.com', 'RECU', NULL),
        ('bema.rasoanaivo@example.com', 'PRESELECTIONNE', NULL),
        ('lova.andriamihaja@example.com', 'TEST_PASSE', NULL),
        ('mamy.randrianasolo@example.com', 'ENTRETIEN_PLANIFIE', NULL),
        ('soa.ravelomanana@example.com', 'RETENU', NULL),
        ('hery.razafindrakoto@example.com', 'EN_ATTENTE', 1)
) AS donnees(email, statut, rang_attente) ON TRUE
JOIN candidat c ON c.email = donnees.email
JOIN cv_candidature cv ON cv.candidat_id = c.id;

INSERT INTO candidature_historique (
    candidature_id, ancien_statut, nouveau_statut, utilisateur_id, commentaire
)
SELECT c.id, NULL, c.statut, rh.id, 'Candidature de démonstration créée.'
FROM candidature c
JOIN utilisateur rh ON rh.email = 'rh@sirh.mg';
