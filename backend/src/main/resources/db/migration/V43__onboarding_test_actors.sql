INSERT INTO role (code, libelle, description)
VALUES
    ('IT', 'Informatique', 'Gestion des comptes et du matériel des salariés'),
    ('SERVICES_GENERAUX', 'Services généraux', 'Gestion des badges et équipements des salariés')
ON CONFLICT (code) DO NOTHING;

-- Comptes de démonstration : DataInitializer remplace le mot de passe brut
-- par un hash BCrypt au démarrage, comme pour les comptes initiaux.
INSERT INTO utilisateur (nom, prenom, email, mot_de_passe, role_id, actif)
SELECT 'Test', 'Informatique', 'it@sirh.mg', 'password', r.id, TRUE
FROM role r WHERE r.code = 'IT'
ON CONFLICT (email) DO NOTHING;

INSERT INTO utilisateur (nom, prenom, email, mot_de_passe, role_id, actif)
SELECT 'Test', 'Services généraux', 'services.generaux@sirh.mg', 'password', r.id, TRUE
FROM role r WHERE r.code = 'SERVICES_GENERAUX'
ON CONFLICT (email) DO NOTHING;

-- Affecte les tâches déjà créées au premier utilisateur actif du rôle concerné.
UPDATE onboarding_tache t
SET assignee_id = (
    SELECT u.id
    FROM utilisateur u
    JOIN role r ON r.id = u.role_id
    WHERE r.code = t.responsable_role AND u.actif = TRUE
    ORDER BY u.id
    LIMIT 1
)
WHERE t.assignee_id IS NULL
  AND EXISTS (
      SELECT 1 FROM utilisateur u JOIN role r ON r.id = u.role_id
      WHERE r.code = t.responsable_role AND u.actif = TRUE
  );
