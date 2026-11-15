-- La date de réalisation peut précéder l'échéance prévue :
-- l'échéance est une cible, pas une contrainte de réalisation.
ALTER TABLE onboarding_tache
    DROP CONSTRAINT chk_onboarding_tache_dates;
