ALTER TABLE planning ADD COLUMN type_avant_conge VARCHAR(20);
ALTER TABLE planning ADD COLUMN source_avant_conge VARCHAR(20);
ALTER TABLE planning ADD COLUMN motif_avant_conge TEXT;
ALTER TABLE planning ADD COLUMN genere_pour_conge BOOLEAN NOT NULL DEFAULT FALSE;
