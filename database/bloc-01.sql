-- 1. Table des Départements
CREATE TABLE departement (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    description TEXT
);

-- Table des Rôles
CREATE TABLE role (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,       -- ex: 'MANAGER', 'RH', 'FINANCE', 'DIRECTION', 'ADMIN'
    libelle VARCHAR(100) NOT NULL,          -- ex: 'Manager', 'Responsable RH'
    description TEXT
);

-- Table des Utilisateurs
CREATE TABLE utilisateur (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    mot_de_passe VARCHAR(255) NOT NULL,
    role_id INT NOT NULL,
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_utilisateur_role FOREIGN KEY (role_id) REFERENCES role(id)
);

-- 3. Table des Postes
CREATE TABLE poste (
    id BIGSERIAL PRIMARY KEY,
    titre VARCHAR(150) NOT NULL,
    departement_id INT NOT NULL,
    description_profil TEXT,
    CONSTRAINT fk_poste_departement FOREIGN KEY (departement_id) REFERENCES departement(id)
);

-- Index sur la clé étrangère
CREATE INDEX idx_poste_departement_id ON poste(departement_id);

-- 4. Table des Demandes de Recrutement
CREATE TABLE demande_recrutement (
    id BIGSERIAL PRIMARY KEY,
    poste_id INT NOT NULL,
    demandeur_id INT NOT NULL,
    validateur_id INT,                          -- peut être NULL tant que non validée
    nombre_postes INT NOT NULL DEFAULT 1,
    type VARCHAR(20) NOT NULL,                  -- 'CREATION' ou 'REMPLACEMENT'
    motif TEXT,                                 -- justification libre
    budget NUMERIC(12, 2) NOT NULL,
    date_souhaitee DATE NOT NULL,
    statut VARCHAR(50) NOT NULL DEFAULT 'EN_ATTENTE_RH',
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_modification TIMESTAMP,
    employe_remplace_id INT,                    -- optionnel, pour les remplacements (activé au bloc 3)

    CONSTRAINT fk_demande_poste FOREIGN KEY (poste_id) REFERENCES poste(id),
    CONSTRAINT fk_demande_demandeur FOREIGN KEY (demandeur_id) REFERENCES utilisateur(id),
    CONSTRAINT fk_demande_validateur FOREIGN KEY (validateur_id) REFERENCES utilisateur(id),

    CONSTRAINT chk_type CHECK (type IN ('CREATION', 'REMPLACEMENT')),
    CONSTRAINT chk_nombre_postes CHECK (nombre_postes > 0),
    CONSTRAINT chk_budget CHECK (budget >= 0)
);

-- Index utiles
CREATE INDEX idx_demande_statut ON demande_recrutement(statut);
CREATE INDEX idx_demande_demandeur_id ON demande_recrutement(demandeur_id);
CREATE INDEX idx_demande_poste_id ON demande_recrutement(poste_id);

-- 5. Table de Traçabilité des Validations (Workflow)
CREATE TABLE validation_demande (
    id BIGSERIAL PRIMARY KEY,
    demande_recrutement_id INT NOT NULL,
    etape VARCHAR(50) NOT NULL,                 -- 'RH', 'FINANCE', 'DIRECTION'
    validateur_id INT NOT NULL,
    statut VARCHAR(30) NOT NULL,                -- 'ACCEPTE', 'REJETE', 'DEMANDE_ANALYSE'
    commentaire TEXT,
    date_validation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_validation_demande FOREIGN KEY (demande_recrutement_id) REFERENCES demande_recrutement(id),
    CONSTRAINT fk_validation_validateur FOREIGN KEY (validateur_id) REFERENCES utilisateur(id)
);

-- Index
CREATE INDEX idx_validation_demande_id ON validation_demande(demande_recrutement_id);