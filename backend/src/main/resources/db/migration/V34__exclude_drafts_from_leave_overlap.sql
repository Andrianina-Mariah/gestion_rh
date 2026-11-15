ALTER TABLE demande_conge DROP CONSTRAINT ex_demande_conge_periodes;
ALTER TABLE demande_conge ADD CONSTRAINT ex_demande_conge_periodes EXCLUDE USING gist (
    employe_id WITH =,
    daterange(date_debut, date_fin + 1, '[)') WITH &&
) WHERE (statut NOT IN ('BROUILLON', 'REFUSEE', 'ANNULEE'));
