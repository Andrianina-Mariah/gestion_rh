-- 1. Table des Offres d'Emploi (issue d'une demande validée)
CREATE TABLE offre_emploi (
    id BIGSERIAL PRIMARY KEY,
    demande_recrutement_id INT NOT NULL,
    titre_offre VARCHAR(200) NOT NULL,
    description_poste TEXT NOT NULL,
    profil_recherche TEXT NOT NULL,
    lieu_travail VARCHAR(100),
    type_contrat_propose VARCHAR(50), -- ex: 'CDD', 'CDI', 'Essai'
    statut VARCHAR(50) DEFAULT 'OUVERTE', -- ex: 'BROUILLON', 'PUBLIEE', 'CLOTUREE'
    date_publication TIMESTAMP,
    date_cloture TIMESTAMP,
    CONSTRAINT fk_offre_demande FOREIGN KEY (demande_recrutement_id) REFERENCES demande_recrutement(id)
);

-- 2. Table des Candidats (Données personnelles de base)
CREATE TABLE candidat (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    telephone VARCHAR(30),
    adresse TEXT,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 1. Table principale du CV structuré (formulaire candidat)
CREATE TABLE cv_candidature (
    id BIGSERIAL PRIMARY KEY,
    candidat_id INT NOT NULL,
    titre_profil VARCHAR(150), -- ex: 'Développeur Fullstack Junior'
    resume TEXT, -- Résumé / Bio / Objectif professionnel
    competences TEXT, -- Liste des compétences sous forme de texte simple
    langues VARCHAR(100),
    date_mise_a_jour TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cv_candidat FOREIGN KEY (candidat_id) REFERENCES candidat(id)
);

-- 2. Table pour les expériences professionnelles liées au CV
CREATE TABLE experience_professionnelle (
    id BIGSERIAL PRIMARY KEY,
    cv_candidature_id INT NOT NULL,
    poste VARCHAR(150) NOT NULL,
    entreprise VARCHAR(150) NOT NULL,
    date_debut DATE,
    date_fin DATE,
    en_poste BOOLEAN DEFAULT FALSE,
    description TEXT,
    CONSTRAINT fk_exp_cv FOREIGN KEY (cv_candidature_id) REFERENCES cv_candidature(id) ON DELETE CASCADE
);

-- 3. Table pour les formations liées au CV
CREATE TABLE formation_candidat (
    id BIGSERIAL PRIMARY KEY,
    cv_candidature_id INT NOT NULL,
    diplome VARCHAR(150) NOT NULL,
    etablissement VARCHAR(150) NOT NULL,
    annee_obtention INT,
    description TEXT,
    CONSTRAINT fk_formation_cv FOREIGN KEY (cv_candidature_id) REFERENCES cv_candidature(id) ON DELETE CASCADE
);

-- 4. Table des Candidatures mise à jour avec la référence vers le CV formulaire
CREATE TABLE candidature (
    id BIGSERIAL PRIMARY KEY,
    offre_emploi_id INT NOT NULL,
    candidat_id INT NOT NULL,
    cv_candidature_id INT, -- Lien vers le CV formulaire structuré
    cv_url VARCHAR(255), -- Lien vers le fichier physique (PDF) si besoin
    lettre_motivation_url VARCHAR(255),
    statut VARCHAR(50) DEFAULT 'RECU', -- ex: 'RECU', 'PRESELECTIONNE', 'EN_ATTENTE', 'REJETE', 'RETENU_TEST'
    date_candidature TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_candidature_offre FOREIGN KEY (offre_emploi_id) REFERENCES offre_emploi(id),
    CONSTRAINT fk_candidature_candidat FOREIGN KEY (candidat_id) REFERENCES candidat(id),
    CONSTRAINT fk_candidature_cv FOREIGN KEY (cv_candidature_id) REFERENCES cv_candidature(id)
);
-- 4. Table des Tests et Évaluations (Phase d'organisation des tests / entretiens)
CREATE TABLE test_recrutement (
    id BIGSERIAL PRIMARY KEY,
    candidature_id INT NOT NULL,
    type_evaluation VARCHAR(50) NOT NULL, -- ex: 'Test Technique', 'Entretien RH', 'Entretien Manager'
    date_passage TIMESTAMP NOT NULL,
    note NUMERIC(5,2),
    remarque TEXT,
    resultat VARCHAR(50) NOT NULL, -- ex: 'ACCEPTE', 'LISTE_D_ATTENTE', 'REJETE'
    evalutateur_id INT, -- ID du manager ou RH qui fait l'évaluation
    CONSTRAINT fk_test_candidature FOREIGN KEY (candidature_id) REFERENCES candidature(id)
);