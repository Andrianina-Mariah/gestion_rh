-- 1. Table des Propositions d'Embauche (Négociation des conditions)
CREATE TABLE proposition_embauche (
    id BIGSERIAL PRIMARY KEY,
    candidature_id INT NOT NULL,
    date_travail_souhaitee DATE NOT NULL,
    lieu_travail VARCHAR(150) NOT NULL,
    salaire_propose NUMERIC(12, 2) NOT NULL,
    type_contrat_propose VARCHAR(50) NOT NULL, -- ex: 'CDI', 'CDD', 'CE'
    statut VARCHAR(50) DEFAULT 'EN_ATTENTE', -- ex: 'EN_ATTENTE', 'ACCEPTE', 'NEGOCIATION', 'REFUSE'
    remarques_candidat TEXT,
    date_proposition TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_prop_candidature FOREIGN KEY (candidature_id) REFERENCES candidature(id)
);

-- 2. Table des Employés (Créée uniquement après acceptation de l'offre : CANDIDAT != EMPLOYÉ)
CREATE TABLE employe (
    id BIGSERIAL PRIMARY KEY,
    candidat_id INT NOT NULL, -- Lien vers le candidat d'origine
    matricule VARCHAR(50) UNIQUE NOT NULL,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    email_professionnel VARCHAR(150),
    telephone_professionnel VARCHAR(30),
    date_embauche DATE NOT NULL,
    statut VARCHAR(50) DEFAULT 'ACTIF', -- ex: 'ACTIF', 'EN_ONBOARDING', 'SORTIE'
    CONSTRAINT fk_employe_candidat FOREIGN KEY (candidat_id) REFERENCES candidat(id)
);

-- 3. Table des Contrats (CE, CDD, CDI - Un salarié peut avoir plusieurs contrats successifs)
CREATE TABLE contrat (
    id BIGSERIAL PRIMARY KEY,
    employe_id INT NOT NULL,
    type_contrat VARCHAR(50) NOT NULL, -- 'CE' (Contrat d'Essai), 'CDD', 'CDI'
    date_debut DATE NOT NULL,
    date_fin DATE, -- NULL si CDI
    salaire NUMERIC(12, 2) NOT NULL,
    statut VARCHAR(50) DEFAULT 'BROUILLON', -- 'CREER', 'SIGNER', 'VALIDE'
    contrat_url VARCHAR(255), -- Fichiers PDF du contrat signé pour la traçabilité
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_contrat_employe FOREIGN KEY (employe_id) REFERENCES employe(id)
);

-- 4. Table des Affectations (Lien entre l'employé, son poste et son département)
CREATE TABLE affectation (
    id BIGSERIAL PRIMARY KEY,
    employe_id INT NOT NULL,
    poste_id INT NOT NULL,
    departement_id INT NOT NULL,
    date_affectation DATE NOT NULL,
    CONSTRAINT fk_aff_employe FOREIGN KEY (employe_id) REFERENCES employe(id),
    CONSTRAINT fk_aff_poste FOREIGN KEY (poste_id) REFERENCES poste(id),
    CONSTRAINT fk_aff_departement FOREIGN KEY (departement_id) REFERENCES departement(id)
);

-- 5. Table d'Onboarding (Checklist d'intégration : badge, matériel, paie, etc.)
CREATE TABLE onboarding_tache (
    id BIGSERIAL PRIMARY KEY,
    employe_id INT NOT NULL,
    intitule_tache VARCHAR(200) NOT NULL, -- ex: 'Création de compte email', 'Attribution badge', 'Inscription paie', 'Remise matériel'
    statut VARCHAR(50) DEFAULT 'A_FAIRE', -- 'A_FAIRE', 'EN_COURS', 'FAIT'
    responsable_rh_id INT,
    date_limite DATE,
    CONSTRAINT fk_onboarding_employe FOREIGN KEY (employe_id) REFERENCES employe(id)
);