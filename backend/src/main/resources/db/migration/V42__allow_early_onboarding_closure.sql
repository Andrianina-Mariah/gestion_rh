ALTER TABLE onboarding DROP CONSTRAINT chk_onboarding_dates;

ALTER TABLE onboarding
    ADD CONSTRAINT chk_onboarding_dates
        CHECK (date_fin_prevue IS NULL OR date_fin_prevue >= date_debut);
