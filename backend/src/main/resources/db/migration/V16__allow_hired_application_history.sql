ALTER TABLE candidature_historique DROP CONSTRAINT chk_historique_nouveau_statut;

ALTER TABLE candidature_historique
    ADD CONSTRAINT chk_historique_nouveau_statut
        CHECK (nouveau_statut IN (
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
