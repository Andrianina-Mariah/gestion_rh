ALTER TABLE mouvement_solde ADD COLUMN cle_idempotence VARCHAR(120);
CREATE UNIQUE INDEX uq_mouvement_solde_idempotence
    ON mouvement_solde(solde_conge_id, cle_idempotence)
    WHERE cle_idempotence IS NOT NULL;

ALTER TABLE type_conge ADD COLUMN solde_negatif_autorise BOOLEAN NOT NULL DEFAULT FALSE;
