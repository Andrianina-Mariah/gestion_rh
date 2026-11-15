INSERT INTO role (code, libelle, description) VALUES
('ADMIN', 'Administrateur', 'Accès complet au système'),
('RH', 'Responsable RH', 'Gestion des demandes de recrutement'),
('FINANCE', 'Responsable Finance', 'Validation budgétaire'),
('DIRECTION', 'Direction', 'Validation finale'),
('MANAGER', 'Manager', 'Création des demandes de recrutement');

INSERT INTO utilisateur (nom, prenom, email, mot_de_passe, role_id) VALUES
('Admin', 'System', 'admin@sirh.mg', 'password', 1),
('Rakoto', 'Jean', 'rh@sirh.mg', 'password', 2),
('Rabe', 'Marie', 'finance@sirh.mg', 'password', 3),
('Andria', 'Paul', 'direction@sirh.mg', 'password', 4),
('Rasoa', 'Sophie', 'manager@sirh.mg', 'password', 5);

INSERT INTO departement (nom, description) VALUES
('Ressources Humaines', 'Gestion du personnel et recrutement'),
('Informatique', 'Développement et infrastructure'),
('Finance', 'Gestion financière et comptabilité');

INSERT INTO poste (titre, departement_id, description_profil) VALUES
('Responsable RH', 1, 'Pilotage de la politique RH'),
('Chargé de recrutement', 1, 'Gestion du processus de recrutement'),
('Développeur Java', 2, 'Développement backend Spring Boot'),
('Développeur Frontend', 2, 'Développement Vue.js'),
('Comptable', 3, 'Tenue de la comptabilité générale');