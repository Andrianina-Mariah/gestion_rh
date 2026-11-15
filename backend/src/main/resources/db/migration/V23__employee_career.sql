ALTER TABLE employe
    ADD COLUMN date_sortie DATE,
    ADD COLUMN motif_sortie VARCHAR(30);

ALTER TABLE employe DROP CONSTRAINT chk_employe_statut;
ALTER TABLE employe ADD CONSTRAINT chk_employe_statut
    CHECK (statut IN ('ACTIF', 'EN_ONBOARDING', 'SORTIE', 'ARCHIVE'));
ALTER TABLE employe ADD CONSTRAINT chk_employe_sortie
    CHECK ((statut IN ('SORTIE', 'ARCHIVE') AND date_sortie IS NOT NULL)
        OR statut NOT IN ('SORTIE', 'ARCHIVE'));
ALTER TABLE employe ADD CONSTRAINT chk_employe_motif_sortie
    CHECK (motif_sortie IS NULL OR motif_sortie IN ('DEMISSION', 'LICENCIEMENT', 'FIN_CONTRAT', 'RETRAITE'));

ALTER TABLE affectation ADD COLUMN manager_id BIGINT;
ALTER TABLE affectation ADD CONSTRAINT fk_affectation_manager
    FOREIGN KEY (manager_id) REFERENCES employe(id);
CREATE INDEX idx_affectation_manager ON affectation(manager_id);

ALTER TABLE historique_salaire
    ADD CONSTRAINT chk_historique_salaire_devise CHECK (devise IN ('MGA', 'EUR', 'USD'));
