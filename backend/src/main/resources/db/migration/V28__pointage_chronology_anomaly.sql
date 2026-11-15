ALTER TABLE anomalie_pointage DROP CONSTRAINT chk_anomalie_pointage_type;
ALTER TABLE anomalie_pointage ADD CONSTRAINT chk_anomalie_pointage_type CHECK (type IN (
    'ENTREE_MANQUANTE', 'SORTIE_MANQUANTE', 'DOUBLON', 'HORS_PLANNING',
    'DUREE_EXCESSIVE', 'RETARD_NON_JUSTIFIE', 'HORODATAGE_INVALIDE'
));
