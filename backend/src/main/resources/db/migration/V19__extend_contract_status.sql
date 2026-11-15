ALTER TABLE contrat
    DROP CONSTRAINT chk_contrat_statut;

ALTER TABLE contrat
    ADD CONSTRAINT chk_contrat_statut
        CHECK (statut IN ('BROUILLON', 'CREE', 'SIGNE', 'VALIDE', 'TERMINE'));
