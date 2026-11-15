ALTER TABLE document
    DROP CONSTRAINT chk_document_type;

ALTER TABLE document
    ADD CONSTRAINT chk_document_type
    CHECK (type IN (
        'CONTRAT', 'BADGE', 'COMPTE', 'MATERIEL', 'FORMATION', 'PAIE', 'AUTRE',
        'CIN', 'PHOTO', 'CV', 'DIPLOME', 'CERTIFICAT_RESIDENCE',
        'CASIER_JUDICIAIRE', 'RIB'
    ));
