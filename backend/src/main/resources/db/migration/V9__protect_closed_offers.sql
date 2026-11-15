-- Une candidature ne peut être déposée que sur une offre publiée.
-- Cette règle est protégée au niveau SQL pour couvrir tous les points d'entrée.

CREATE OR REPLACE FUNCTION verifier_offre_publiee_pour_candidature()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM offre_emploi
        WHERE id = NEW.offre_emploi_id
          AND statut = 'PUBLIEE'
    ) THEN
        RAISE EXCEPTION 'Une candidature ne peut être déposée que sur une offre PUBLIEE';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_candidature_offre_publiee
BEFORE INSERT ON candidature
FOR EACH ROW
EXECUTE FUNCTION verifier_offre_publiee_pour_candidature();
