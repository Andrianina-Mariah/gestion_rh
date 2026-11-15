ALTER TABLE absence ADD COLUMN anomalie_id BIGINT REFERENCES anomalie_pointage(id);
ALTER TABLE absence ADD COLUMN date_limite_justificatif DATE;
CREATE UNIQUE INDEX uq_absence_anomalie ON absence(anomalie_id) WHERE anomalie_id IS NOT NULL;
CREATE INDEX idx_absence_justificatif_echeance ON absence(statut, date_limite_justificatif);

CREATE TABLE politique_absence (
    id SMALLINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    delai_justificatif_jours INT NOT NULL DEFAULT 3 CHECK (delai_justificatif_jours >= 0),
    taille_max_justificatif_octets BIGINT NOT NULL DEFAULT 5242880 CHECK (taille_max_justificatif_octets > 0)
);
INSERT INTO politique_absence(id) VALUES (1);

CREATE TABLE absence_rappel_justificatif (
    id BIGSERIAL PRIMARY KEY,
    absence_id BIGINT NOT NULL REFERENCES absence(id),
    date_rappel DATE NOT NULL,
    CONSTRAINT uq_absence_rappel_jour UNIQUE(absence_id, date_rappel)
);

INSERT INTO type_document(code,libelle,obligatoire,confidentialite)
VALUES('JUSTIFICATIF_ABSENCE','Justificatif d''absence',FALSE,'SENSIBLE')
ON CONFLICT(code) DO NOTHING;
