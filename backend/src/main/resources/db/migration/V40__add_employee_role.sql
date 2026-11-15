INSERT INTO role (code, libelle, description)
VALUES ('EMPLOYE', 'Salarié', 'Accès salarié à son dossier et aux services RH')
ON CONFLICT (code) DO NOTHING;
