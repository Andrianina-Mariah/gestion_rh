CREATE TABLE politique_demande_conge (
    id SMALLINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    preavis_min_jours INT NOT NULL DEFAULT 0 CHECK (preavis_min_jours >= 0),
    presence_min_pct NUMERIC(5,2) CHECK (presence_min_pct IS NULL OR presence_min_pct BETWEEN 0 AND 100)
);
INSERT INTO politique_demande_conge(id) VALUES (1);

CREATE TABLE periode_blocage_conge (
    id BIGSERIAL PRIMARY KEY,
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    libelle VARCHAR(200) NOT NULL,
    CONSTRAINT chk_periode_blocage_conge_dates CHECK (date_fin >= date_debut)
);
CREATE INDEX idx_periode_blocage_conge_dates ON periode_blocage_conge(date_debut, date_fin);

