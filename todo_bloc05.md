# TODO - SIRH - Bloc 5 : Dossier administratif du salarié

Stack : Java 21 (Spring Boot) / Vue.js 3 / PostgreSQL

Prérequis : blocs 3 et 4 terminés (employé, contrats, affectations, documents, `OnboardingTermineEvent`).

Périmètre : identité, coordonnées, documents administratifs et validité, situation professionnelle, historique de carrière (contrats, affectations, salaire), confidentialité, habilitations, journal d'audit.
Hors périmètre : calcul de la paie, pointage (bloc 6), congés (bloc 7).

Légende : [ ] à faire, [x] terminé

---

## 0. Décisions de conception

- [ ] Règle centrale : le dossier salarié est le référentiel unique du SIRH ; les autres blocs lisent ses données et ne les dupliquent pas
- [ ] Identité : lire nom, prénom, téléphone depuis `candidat` ou les copier dans `employe` une fois pour toutes ? (décision à documenter ; si copie, `employe` devient la source de vérité après l'embauche)
- [ ] Historisation : toute donnée qui change dans le temps (adresse, situation familiale, salaire, poste) garde son historique avec `date_debut` / `date_fin` plutôt qu'un écrasement
- [ ] Données sensibles : salaire, pièce d'identité (CIN), numéro CNaPS, situation familiale, coordonnées bancaires, dossier médical ; niveau de confidentialité à définir par champ
- [ ] Rôles : EMPLOYE (son propre dossier), MANAGER (son équipe, données limitées), RH (tout sauf données très sensibles ?), ADMIN, AUDITEUR (lecture du journal) ; matrice des habilitations à valider
- [ ] Modification par l'employé : ce qu'il peut modifier seul (adresse, téléphone, contact d'urgence) et ce qui passe par une validation RH
- [ ] Documents : liste des types obligatoires (CIN, photo, CV, diplômes, certificat de résidence, casier judiciaire, RIB, etc.) et leur durée de validité éventuelle
- [ ] Conservation et suppression : durée de conservation après départ du salarié ; archivage plutôt que suppression
- [ ] Machine à états d'un document : `DEPOSE -> VALIDE | REJETE`, puis `EXPIRE` automatiquement à la date de validité

## 1. Base de données

- [ ] `employe` : compléter l'identité (date et lieu de naissance, sexe, nationalité, CIN, numéro CNaPS, situation familiale, nombre d'enfants) ; `CHECK` sur les valeurs énumérées
- [ ] `employe_adresse` : `employe_id`, `type` (PRINCIPALE, SECONDAIRE), `adresse`, `ville`, `date_debut`, `date_fin` (historique)
- [ ] `employe_contact` : téléphones et emails personnels, avec un seul contact principal par type
- [ ] `contact_urgence` : nom, lien de parenté, téléphone
- [ ] `ayant_droit` (enfants, conjoint) : nom, lien, date de naissance (utile pour la paie et les avantages)
- [ ] `document` : ajouter `statut`, `date_emission`, `date_expiration`, `valide_par_id`, `date_validation`, `motif_rejet`, `confidentialite`, `version`
- [ ] `type_document` : `code`, `libelle`, `obligatoire`, `duree_validite_mois`, `confidentialite`
- [ ] `historique_salaire` : `employe_id`, `montant`, `devise`, `date_debut`, `date_fin`, `motif`, `contrat_id`
- [ ] `diplome` / `experience` (facultatif) : formation et parcours antérieur
- [ ] `journal_audit` : `id`, `date_heure`, `utilisateur_id`, `role`, `action` (LECTURE, CREATION, MODIFICATION, SUPPRESSION, EXPORT), `entite`, `entite_id`, `ancienne_valeur` (JSONB), `nouvelle_valeur` (JSONB), `adresse_ip`
- [ ] Journal d'audit en insertion seule : aucun UPDATE ni DELETE autorisé (droits PostgreSQL ou trigger)
- [ ] Contraintes : un seul contact principal, un seul salaire courant (`date_fin` nulle), `date_fin >= date_debut`, pas de chevauchement de périodes
- [ ] Index : `document(employe_id, type_document_id)`, `document(date_expiration)`, `journal_audit(entite, entite_id)`, `journal_audit(utilisateur_id, date_heure)`
- [ ] Migrations Flyway et jeu de test

## 2. Backend : dossier et identité

- [ ] Entités et DTO : identité, adresses, contacts, contacts d'urgence, ayants droit
- [ ] `GET /api/employes/{id}/dossier` : vue consolidée (identité, coordonnées, situation professionnelle, documents, contrats, affectations, salaire)
- [ ] `PUT /api/employes/{id}/identite` et endpoints de coordonnées, avec historisation des changements d'adresse
- [ ] Auto-service : `GET` et `PUT /api/moi/dossier` limités aux champs autorisés ; les autres changements créent une demande à valider par la RH
- [ ] Demande de modification : table `demande_modification` (champ, ancienne valeur, nouvelle valeur, statut) et validation RH
- [ ] Contrôles de format : CIN, numéro CNaPS, téléphone, email, date de naissance cohérente (âge légal de travail)
- [ ] Recherche avancée : nom, matricule, département, poste, statut ; pagination et tri

## 3. Backend : situation professionnelle et carrière

- [ ] Vue "situation actuelle" : poste, département, manager, contrat actif, salaire courant, ancienneté
- [ ] Calcul de l'ancienneté à partir de la première date de contrat
- [ ] Historique de carrière chronologique fusionnant contrats, affectations (mutations, promotions) et changements de salaire : `GET /api/employes/{id}/carriere`
- [ ] Mutation / changement de poste : `POST /api/employes/{id}/affectations` clôt l'affectation courante (`date_fin`) et en ouvre une nouvelle, dans une seule transaction
- [ ] Changement de salaire : `POST /api/employes/{id}/salaires`, historique conservé, lien avec le contrat ou un avenant
- [ ] Gestion du manager hiérarchique et organigramme (`manager_id`), sans cycle
- [ ] Départ du salarié : statuts (DEMISSION, LICENCIEMENT, FIN_CONTRAT, RETRAITE), date de sortie, passage du dossier en ARCHIVE
- [ ] Événements : `AffectationChangeeEvent`, `SalaireChangeEvent`, `EmployeSortiEvent` (pour les blocs suivants)

## 4. Backend : documents administratifs

- [ ] Dépôt : `POST /api/employes/{id}/documents` avec contrôle du type MIME, de la taille et antivirus si possible
- [ ] Stockage hors base (système de fichiers ou stockage objet), nom de fichier généré, jamais le nom d'origine dans le chemin
- [ ] Téléchargement sécurisé : contrôle d'habilitation à chaque accès, URL non devinable ou signée à durée limitée
- [ ] Validation / rejet par la RH avec motif (`POST /api/documents/{id}/valider`, `/rejeter`)
- [ ] Versionnement : un nouveau dépôt du même type crée une version, l'ancienne est conservée
- [ ] Validité : tâche planifiée quotidienne qui passe en EXPIRE les documents échus et signale ceux qui expirent dans N jours
- [ ] Liste des documents manquants par employé (types obligatoires non fournis ou expirés) : `GET /api/employes/{id}/documents/manquants`
- [ ] Rapport global des documents manquants ou expirants pour la RH
- [ ] Notifications : document rejeté, document à renouveler, document manquant

## 5. Backend : confidentialité, habilitations et audit

- [ ] Habilitations par rôle et par donnée : filtrage côté serveur (jamais seulement côté Vue), DTO différents selon le rôle
- [ ] Contrôle d'accès à la ligne : un EMPLOYE ne lit que son dossier, un MANAGER que son équipe
- [ ] Masquage des champs sensibles (salaire, CIN, RIB) pour les rôles non autorisés
- [ ] Journal d'audit alimenté automatiquement (intercepteur ou listener JPA) pour création, modification, suppression
- [ ] Audit des lectures de données sensibles (consultation du salaire, téléchargement d'un document confidentiel)
- [ ] Audit des exports
- [ ] `GET /api/audit` avec filtres (utilisateur, entité, période, action), réservé aux rôles ADMIN et AUDITEUR
- [ ] Anonymisation ou archivage à l'expiration de la durée de conservation
- [ ] Respect du principe de minimisation : ne renvoyer que les champs nécessaires

## 6. Frontend

### 6.1 Espace RH

- [ ] Liste des employés avec recherche et filtres, export contrôlé
- [ ] Fiche dossier à onglets : Identité, Coordonnées, Situation professionnelle, Contrats, Affectations et salaire, Documents, Audit
- [ ] Frise de carrière
- [ ] Gestion des documents : dépôt, aperçu, validation / rejet, versions, alertes d'expiration
- [ ] Tableau de bord : documents manquants ou expirant, demandes de modification à traiter
- [ ] Formulaires de mutation et de changement de salaire
- [ ] Consultation du journal d'audit (ADMIN, AUDITEUR)

### 6.2 Espace salarié

- [ ] "Mon dossier" : consultation et modification des champs autorisés
- [ ] Dépôt de ses documents, suivi de leur statut
- [ ] Suivi de ses demandes de modification

### 6.3 Espace manager

- [ ] Vue de son équipe (infos professionnelles limitées, pas de données sensibles)
- [ ] Organigramme

## 7. Tests

- [ ] Unitaires : historisation des adresses et salaires, absence de chevauchement, calcul de l'ancienneté, expiration des documents
- [ ] Intégration : une mutation clôt l'ancienne affectation et ouvre la nouvelle en une transaction
- [ ] Sécurité : matrice complète rôle x ressource (accès autorisé et refusé), tentative d'accès au dossier d'un autre employé
- [ ] Audit : chaque modification produit une ligne avec ancienne et nouvelle valeur ; le journal ne peut être ni modifié ni supprimé
- [ ] Documents : fichier trop gros, type interdit, téléchargement sans droit, expiration automatique
- [ ] Bout en bout : fin d'onboarding -> dossier complété -> documents validés -> mutation -> historique de carrière correct

## 8. Documentation

- [ ] Matrice des habilitations (rôle x donnée x action)
- [ ] Liste des types de documents et durées de validité
- [ ] Politique de confidentialité et de conservation
- [ ] Règles de gestion du bloc 5 (suite de la numérotation)
- [ ] Schéma de base et OpenAPI à jour

## 9. Critères de fin

- [ ] Le dossier centralise identité, coordonnées, documents et situation professionnelle
- [ ] Contrats, affectations et salaires sont conservés avec leur historique de carrière
- [ ] Les documents ont un statut et une validité, les expirations sont signalées
- [ ] Les accès sont filtrés par rôle et par employé
- [ ] Chaque modification et chaque consultation sensible est tracée dans un journal d'audit non modifiable
- [ ] Les tests passent, la documentation est à jour

---

## Ordre d'exécution recommandé

1. Sections 0 et 1 (décisions, migrations)
2. Section 5 (habilitations et audit : à poser tôt, car tout le reste en dépend)
3. Sections 2 et 3 (dossier, carrière), puis 6.1 et 6.2
4. Section 4 (documents), puis la partie documents du frontend
5. Sections 7, 8, 9
