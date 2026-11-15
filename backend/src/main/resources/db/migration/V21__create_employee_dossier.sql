-- Bloc 5 : dossier salarié, historique, documents et audit.
CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE employe
    ADD COLUMN date_naissance DATE,
    ADD COLUMN lieu_naissance VARCHAR(150),
    ADD COLUMN sexe VARCHAR(20),
    ADD COLUMN nationalite VARCHAR(80),
    ADD COLUMN cin VARCHAR(50),
    ADD COLUMN numero_cnaps VARCHAR(50),
    ADD COLUMN situation_familiale VARCHAR(30),
    ADD COLUMN nombre_enfants INT NOT NULL DEFAULT 0;

ALTER TABLE employe
    ADD CONSTRAINT chk_employe_sexe
        CHECK (sexe IS NULL OR sexe IN ('FEMME', 'HOMME', 'AUTRE')),
    ADD CONSTRAINT chk_employe_situation_familiale
        CHECK (situation_familiale IS NULL OR situation_familiale IN
            ('CELIBATAIRE', 'MARIE', 'DIVORCE', 'VEUF', 'AUTRE')),
    ADD CONSTRAINT chk_employe_nombre_enfants CHECK (nombre_enfants >= 0);

CREATE UNIQUE INDEX uq_employe_cin ON employe(cin) WHERE cin IS NOT NULL;
CREATE UNIQUE INDEX uq_employe_cnaps ON employe(numero_cnaps) WHERE numero_cnaps IS NOT NULL;

CREATE TABLE employe_adresse (
    id BIGSERIAL PRIMARY KEY,
    employe_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    adresse VARCHAR(500) NOT NULL,
    ville VARCHAR(120),
    date_debut DATE NOT NULL,
    date_fin DATE,
    CONSTRAINT fk_employe_adresse_employe FOREIGN KEY (employe_id) REFERENCES employe(id),
    CONSTRAINT chk_employe_adresse_type CHECK (type IN ('PRINCIPALE', 'SECONDAIRE')),
    CONSTRAINT chk_employe_adresse_dates CHECK (date_fin IS NULL OR date_fin >= date_debut)
);

CREATE INDEX idx_employe_adresse_employe ON employe_adresse(employe_id, date_debut);
ALTER TABLE employe_adresse
    ADD CONSTRAINT ex_employe_adresse_periodes
    EXCLUDE USING gist (
        employe_id WITH =,
        type WITH =,
        daterange(date_debut, COALESCE(date_fin + 1, 'infinity'::date), '[)') WITH &&
    );

CREATE TABLE employe_contact (
    id BIGSERIAL PRIMARY KEY,
    employe_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    valeur VARCHAR(180) NOT NULL,
    principal BOOLEAN NOT NULL DEFAULT FALSE,
    date_debut DATE NOT NULL,
    date_fin DATE,
    CONSTRAINT fk_employe_contact_employe FOREIGN KEY (employe_id) REFERENCES employe(id),
    CONSTRAINT chk_employe_contact_type CHECK (type IN ('TELEPHONE', 'EMAIL')),
    CONSTRAINT chk_employe_contact_dates CHECK (date_fin IS NULL OR date_fin >= date_debut)
);

CREATE UNIQUE INDEX uq_employe_contact_principal
    ON employe_contact(employe_id, type) WHERE principal AND date_fin IS NULL;
CREATE INDEX idx_employe_contact_employe ON employe_contact(employe_id);

CREATE TABLE contact_urgence (
    id BIGSERIAL PRIMARY KEY,
    employe_id BIGINT NOT NULL,
    nom VARCHAR(150) NOT NULL,
    lien_parente VARCHAR(80) NOT NULL,
    telephone VARCHAR(30) NOT NULL,
    principal BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_contact_urgence_employe FOREIGN KEY (employe_id) REFERENCES employe(id)
);

CREATE UNIQUE INDEX uq_contact_urgence_principal
    ON contact_urgence(employe_id) WHERE principal;

CREATE TABLE ayant_droit (
    id BIGSERIAL PRIMARY KEY,
    employe_id BIGINT NOT NULL,
    nom VARCHAR(150) NOT NULL,
    lien VARCHAR(50) NOT NULL,
    date_naissance DATE,
    CONSTRAINT fk_ayant_droit_employe FOREIGN KEY (employe_id) REFERENCES employe(id)
);

CREATE TABLE type_document (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    libelle VARCHAR(150) NOT NULL,
    obligatoire BOOLEAN NOT NULL DEFAULT FALSE,
    duree_validite_mois INT,
    confidentialite VARCHAR(20) NOT NULL DEFAULT 'INTERNE',
    CONSTRAINT chk_type_document_duree CHECK (duree_validite_mois IS NULL OR duree_validite_mois > 0),
    CONSTRAINT chk_type_document_confidentialite CHECK (confidentialite IN ('PUBLIC', 'INTERNE', 'SENSIBLE', 'TRES_SENSIBLE'))
);

INSERT INTO type_document (code, libelle, obligatoire, confidentialite) VALUES
    ('CIN', 'Pièce d''identité', TRUE, 'TRES_SENSIBLE'),
    ('PHOTO', 'Photo', FALSE, 'SENSIBLE'),
    ('CV', 'Curriculum vitae', TRUE, 'INTERNE'),
    ('DIPLOME', 'Diplôme', FALSE, 'INTERNE'),
    ('CERTIFICAT_RESIDENCE', 'Certificat de résidence', FALSE, 'SENSIBLE'),
    ('CASIER_JUDICIAIRE', 'Casier judiciaire', FALSE, 'TRES_SENSIBLE'),
    ('RIB', 'Relevé d''identité bancaire', TRUE, 'TRES_SENSIBLE');

ALTER TABLE document
    ADD COLUMN type_document_id BIGINT,
    ADD COLUMN statut VARCHAR(20) NOT NULL DEFAULT 'DEPOSE',
    ADD COLUMN date_emission DATE,
    ADD COLUMN date_expiration DATE,
    ADD COLUMN valide_par_id INT,
    ADD COLUMN date_validation TIMESTAMP,
    ADD COLUMN motif_rejet TEXT,
    ADD COLUMN confidentialite VARCHAR(20) NOT NULL DEFAULT 'INTERNE',
    ADD COLUMN version INT NOT NULL DEFAULT 1;

ALTER TABLE document
    ADD CONSTRAINT fk_document_type_document FOREIGN KEY (type_document_id) REFERENCES type_document(id),
    ADD CONSTRAINT fk_document_valide_par FOREIGN KEY (valide_par_id) REFERENCES utilisateur(id),
    ADD CONSTRAINT chk_document_statut CHECK (statut IN ('DEPOSE', 'VALIDE', 'REJETE', 'EXPIRE')),
    ADD CONSTRAINT chk_document_dates CHECK (date_expiration IS NULL OR date_emission IS NULL OR date_expiration >= date_emission),
    ADD CONSTRAINT chk_document_confidentialite CHECK (confidentialite IN ('PUBLIC', 'INTERNE', 'SENSIBLE', 'TRES_SENSIBLE')),
    ADD CONSTRAINT chk_document_version CHECK (version > 0);

CREATE INDEX idx_document_employe_type ON document(employe_id, type_document_id);
CREATE INDEX idx_document_expiration ON document(date_expiration);

CREATE TABLE historique_salaire (
    id BIGSERIAL PRIMARY KEY,
    employe_id BIGINT NOT NULL,
    montant NUMERIC(15, 2) NOT NULL,
    devise VARCHAR(3) NOT NULL DEFAULT 'MGA',
    date_debut DATE NOT NULL,
    date_fin DATE,
    motif VARCHAR(150),
    contrat_id BIGINT,
    CONSTRAINT fk_historique_salaire_employe FOREIGN KEY (employe_id) REFERENCES employe(id),
    CONSTRAINT fk_historique_salaire_contrat FOREIGN KEY (contrat_id) REFERENCES contrat(id),
    CONSTRAINT chk_historique_salaire_montant CHECK (montant >= 0),
    CONSTRAINT chk_historique_salaire_dates CHECK (date_fin IS NULL OR date_fin >= date_debut)
);

CREATE UNIQUE INDEX uq_historique_salaire_courant ON historique_salaire(employe_id) WHERE date_fin IS NULL;
ALTER TABLE historique_salaire
    ADD CONSTRAINT ex_historique_salaire_periodes
    EXCLUDE USING gist (
        employe_id WITH =,
        daterange(date_debut, COALESCE(date_fin + 1, 'infinity'::date), '[)') WITH &&
    );

CREATE TABLE journal_audit (
    id BIGSERIAL PRIMARY KEY,
    date_heure TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    utilisateur_id INT,
    role VARCHAR(50),
    action VARCHAR(20) NOT NULL,
    entite VARCHAR(100) NOT NULL,
    entite_id BIGINT,
    ancienne_valeur JSONB,
    nouvelle_valeur JSONB,
    adresse_ip INET,
    CONSTRAINT fk_journal_audit_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES utilisateur(id),
    CONSTRAINT chk_journal_audit_action CHECK (action IN ('LECTURE', 'CREATION', 'MODIFICATION', 'SUPPRESSION', 'EXPORT'))
);

CREATE INDEX idx_journal_audit_entite ON journal_audit(entite, entite_id);
CREATE INDEX idx_journal_audit_utilisateur_date ON journal_audit(utilisateur_id, date_heure);

CREATE OR REPLACE FUNCTION journal_audit_append_only()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'journal_audit est une table append-only : modification et suppression interdites';
END;
$$;

CREATE TRIGGER trg_journal_audit_append_only
BEFORE UPDATE OR DELETE ON journal_audit
FOR EACH ROW EXECUTE FUNCTION journal_audit_append_only();
