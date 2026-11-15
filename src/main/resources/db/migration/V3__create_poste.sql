CREATE TABLE poste (
    id BIGSERIAL PRIMARY KEY,
    titre VARCHAR(150) NOT NULL,
    departement_id INT NOT NULL,
    description_profil TEXT,

    CONSTRAINT fk_poste_departement FOREIGN KEY (departement_id) REFERENCES departement(id)
);

CREATE INDEX idx_poste_departement_id ON poste(departement_id);