ALTER TABLE evaluation DROP CONSTRAINT chk_evaluation_type;
ALTER TABLE evaluation
    ADD CONSTRAINT chk_evaluation_type
        CHECK (type_evaluation IN ('TEST_TECHNIQUE', 'ENTRETIEN_RH', 'ENTRETIEN_MANAGER'));
