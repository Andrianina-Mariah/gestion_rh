-- Table des rôles
CREATE TABLE role (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL,
    description TEXT
);

-- Table des utilisateurs
CREATE TABLE utilisateur (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    mot_de_passe VARCHAR(255) NOT NULL,
    role_id INT NOT NULL,
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_utilisateur_role FOREIGN KEY (role_id) REFERENCES role(id)
);

CREATE INDEX idx_utilisateur_role_id ON utilisateur(role_id);
CREATE INDEX idx_utilisateur_email ON utilisateur(email);