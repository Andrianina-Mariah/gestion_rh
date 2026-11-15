ALTER TABLE planning ADD COLUMN motif TEXT;

ALTER TABLE planning
    ADD CONSTRAINT chk_planning_motif_modification
    CHECK (source <> 'MANUEL' OR (motif IS NOT NULL AND length(btrim(motif)) > 0));
