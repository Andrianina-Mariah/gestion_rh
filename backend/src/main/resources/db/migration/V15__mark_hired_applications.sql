ALTER TABLE candidature DROP CONSTRAINT chk_candidature_statut;

ALTER TABLE candidature
    ADD CONSTRAINT chk_candidature_statut
        CHECK (statut IN (
            'RECU',
            'PRESELECTIONNE',
            'EN_ATTENTE',
            'REJETE',
            'TEST_PLANIFIE',
            'TEST_PASSE',
            'ENTRETIEN_PLANIFIE',
            'ENTRETIEN_PASSE',
            'RETENU',
            'EMBAUCHE'
        ));
