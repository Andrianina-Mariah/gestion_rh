ALTER TABLE horaire_type
    ALTER COLUMN jour_semaine TYPE INTEGER
    USING jour_semaine::INTEGER;
