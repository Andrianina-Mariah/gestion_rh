-- Les tables du bloc 2 utilisent BIGSERIAL. Les clés étrangères doivent
-- utiliser BIGINT pour rester cohérentes avec ddl-auto=validate.

ALTER TABLE cv_candidature DROP CONSTRAINT fk_cv_candidat;
ALTER TABLE experience_professionnelle DROP CONSTRAINT fk_experience_cv;
ALTER TABLE formation_candidat DROP CONSTRAINT fk_formation_cv;
ALTER TABLE candidature DROP CONSTRAINT fk_candidature_offre;
ALTER TABLE candidature DROP CONSTRAINT fk_candidature_candidat;
ALTER TABLE candidature DROP CONSTRAINT fk_candidature_cv;
ALTER TABLE candidature_historique DROP CONSTRAINT fk_candidature_historique_candidature;
ALTER TABLE candidature_historique DROP CONSTRAINT fk_candidature_historique_utilisateur;
ALTER TABLE evaluation DROP CONSTRAINT fk_evaluation_candidature;

ALTER TABLE cv_candidature
    ALTER COLUMN candidat_id TYPE BIGINT USING candidat_id::BIGINT;
ALTER TABLE experience_professionnelle
    ALTER COLUMN cv_candidature_id TYPE BIGINT USING cv_candidature_id::BIGINT;
ALTER TABLE formation_candidat
    ALTER COLUMN cv_candidature_id TYPE BIGINT USING cv_candidature_id::BIGINT;
ALTER TABLE candidature
    ALTER COLUMN offre_emploi_id TYPE BIGINT USING offre_emploi_id::BIGINT,
    ALTER COLUMN candidat_id TYPE BIGINT USING candidat_id::BIGINT,
    ALTER COLUMN cv_candidature_id TYPE BIGINT USING cv_candidature_id::BIGINT;
ALTER TABLE candidature_historique
    ALTER COLUMN candidature_id TYPE BIGINT USING candidature_id::BIGINT,
    ALTER COLUMN utilisateur_id TYPE BIGINT USING utilisateur_id::BIGINT;
ALTER TABLE evaluation
    ALTER COLUMN candidature_id TYPE BIGINT USING candidature_id::BIGINT;

ALTER TABLE cv_candidature
    ADD CONSTRAINT fk_cv_candidat FOREIGN KEY (candidat_id) REFERENCES candidat(id);
ALTER TABLE experience_professionnelle
    ADD CONSTRAINT fk_experience_cv FOREIGN KEY (cv_candidature_id) REFERENCES cv_candidature(id) ON DELETE CASCADE;
ALTER TABLE formation_candidat
    ADD CONSTRAINT fk_formation_cv FOREIGN KEY (cv_candidature_id) REFERENCES cv_candidature(id) ON DELETE CASCADE;
ALTER TABLE candidature
    ADD CONSTRAINT fk_candidature_offre FOREIGN KEY (offre_emploi_id) REFERENCES offre_emploi(id),
    ADD CONSTRAINT fk_candidature_candidat FOREIGN KEY (candidat_id) REFERENCES candidat(id),
    ADD CONSTRAINT fk_candidature_cv FOREIGN KEY (cv_candidature_id) REFERENCES cv_candidature(id);
ALTER TABLE candidature_historique
    ADD CONSTRAINT fk_candidature_historique_candidature FOREIGN KEY (candidature_id) REFERENCES candidature(id),
    ADD CONSTRAINT fk_candidature_historique_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES utilisateur(id);
ALTER TABLE evaluation
    ADD CONSTRAINT fk_evaluation_candidature FOREIGN KEY (candidature_id) REFERENCES candidature(id);
