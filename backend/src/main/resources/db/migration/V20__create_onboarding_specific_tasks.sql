ALTER TABLE onboarding_tache
    ADD COLUMN badge_numero VARCHAR(80),
    ADD COLUMN badge_date_remise DATE;

CREATE UNIQUE INDEX uq_onboarding_badge_numero
    ON onboarding_tache(badge_numero)
    WHERE badge_numero IS NOT NULL;

CREATE TABLE materiel_attribue (
    id BIGSERIAL PRIMARY KEY,
    employe_id BIGINT NOT NULL,
    onboarding_tache_id BIGINT,
    type_materiel VARCHAR(80) NOT NULL,
    description VARCHAR(200),
    numero_serie VARCHAR(120) NOT NULL,
    date_attribution DATE NOT NULL DEFAULT CURRENT_DATE,

    CONSTRAINT fk_materiel_employe FOREIGN KEY (employe_id) REFERENCES employe(id),
    CONSTRAINT fk_materiel_tache FOREIGN KEY (onboarding_tache_id) REFERENCES onboarding_tache(id),
    CONSTRAINT uq_materiel_numero_serie UNIQUE (numero_serie)
);

CREATE INDEX idx_materiel_employe ON materiel_attribue(employe_id);

CREATE TABLE formation_accueil (
    id BIGSERIAL PRIMARY KEY,
    employe_id BIGINT NOT NULL,
    onboarding_id BIGINT NOT NULL,
    onboarding_tache_id BIGINT,
    date_formation DATE NOT NULL,
    duree_heures NUMERIC(6,2) NOT NULL,
    formateur VARCHAR(150) NOT NULL,

    CONSTRAINT fk_formation_employe FOREIGN KEY (employe_id) REFERENCES employe(id),
    CONSTRAINT fk_formation_onboarding FOREIGN KEY (onboarding_id) REFERENCES onboarding(id),
    CONSTRAINT fk_formation_tache FOREIGN KEY (onboarding_tache_id) REFERENCES onboarding_tache(id),
    CONSTRAINT chk_formation_duree CHECK (duree_heures > 0)
);

CREATE INDEX idx_formation_employe ON formation_accueil(employe_id);
