# TODO - SIRH - Bloc 1 : Expression du besoin en personnel

## Partie 1 : Fondations, référentiels et création de la demande

Stack : Java 21 (Spring Boot) / Vue.js 3 / PostgreSQL

Périmètre de la partie 1 :

- Mise en place du projet (back, front, base)
- Référentiels Département et Poste
- Utilisateurs et rôles minimum (Manager, RH, Finance, Direction)
- Création, soumission et consultation d'une DemandeRecrutement par le Manager

Hors périmètre (partie 2) : circuit de validation RH -> Finance -> Direction et ouverture du recrutement.

Légende : [ ] à faire, [x] terminé

---

## 0. Décisions de conception (avant de coder)

- [ ] Confirmer que POSTE est indépendant de EMPLOYÉ (un poste existe sans titulaire)
- [ ] Définir la liste des rôles : MANAGER, RH, FINANCE, DIRECTION, ADMIN
- [ ] Définir les statuts de la demande : BROUILLON, EN_ATTENTE_RH, ANALYSE_FINANCE, VALIDE, REJETE
- [ ] Définir les règles de gestion (voir section 5)
- [ ] Choisir l'architecture back : couches controller / service / repository / domain
- [ ] Choisir les conventions : nommage des packages, des endpoints REST, format des erreurs JSON
- [ ] Choisir la gestion de l'authentification (JWT recommandé)

## 1. Initialisation des projets

### 1.1 Base de données

- [X] Installer PostgreSQL et créer la base `sirh`
- [X] Créer un utilisateur applicatif dédié (droits limités)
- [X] Prévoir un fichier `docker-compose.yml` pour PostgreSQL (facultatif mais conseillé)

### 1.2 Backend (Java 21)

- [X] Créer le projet Spring Boot (Maven ou Gradle) avec Java 21
- [X] Dépendances : Web, Data JPA, Validation, Security, PostgreSQL driver, Flyway, Lombok, springdoc-openapi
- [X] Configurer `application.yml` (profils dev / prod, connexion base)
- [X] Créer l'arborescence de packages :
  - `config`, `security`, `common` (exceptions, dto communs)
  - `departement`, `poste`, `utilisateur`, `demanderecrutement`
  - chaque module contient : `controller`, `service`, `repository`, `entity`, `dto`, `mapper`
- [X] Mettre en place un gestionnaire global d'exceptions (`@RestControllerAdvice`)
- [X] Activer Swagger / OpenAPI pour documenter l'API
- [X] Mettre en place CORS pour le front

### 1.3 Frontend (Vue.js)

- [X] Créer le projet Vue 3 avec Vite
- [X] Ajouter : Vue Router, Pinia, Axios, une bibliothèque UI (PrimeVue ou Vuetify)
- [X] Créer l'arborescence :
  - `api/` (appels Axios par module)
  - `stores/` (Pinia)
  - `views/`, `components/`, `router/`, `layouts/`
- [X] Configurer l'instance Axios (URL de base, intercepteur du token, gestion des erreurs)
- [X] Créer le layout principal (menu latéral, en-tête, zone de contenu)

## 2. Base de données : migrations Flyway

### 2.1 Corrections à faire sur le script `bloc-01.sql`

- [X] Remplacer `ENUM("creation", "remplacement")` (syntaxe MySQL, invalide en PostgreSQL) par un `VARCHAR` avec contrainte `CHECK`, ou un type ENUM PostgreSQL
- [X] Supprimer la redondance entre `type` et `motif` : garder `type` (CREATION / REMPLACEMENT) et utiliser `motif` pour le texte libre de justification
- [X] Ajouter la table `utilisateur` et la clé étrangère de `demandeur_id` et `validateur_id`
- [X] Ajouter une contrainte `CHECK (nombre_postes > 0)` et `CHECK (budget >= 0)`
- [X] Ajouter `date_modification` sur `demande_recrutement`
- [X] Ajouter des index sur `poste.departement_id`, `demande_recrutement.statut`, `demande_recrutement.demandeur_id`
- [X] Ajouter `departement_id` sur la demande si le département doit pouvoir différer de celui du poste (sinon le déduire du poste)
- [X] Dans le cas d'un remplacement, prévoir un champ optionnel `employe_remplace_id` (activé au bloc 3)

### 2.2 Migrations

- [X] `V1__create_utilisateur_role.sql` : tables `utilisateur`, `role`
- [X] `V2__create_departement.sql`
- [X] `V3__create_poste.sql`
- [X] `V4__create_demande_recrutement.sql`
- [X] `V5__create_validation_demande.sql` (structure seulement, utilisée en partie 2)
- [X] `V6__seed_data.sql` : rôles, un utilisateur par rôle, 3 départements, 5 postes
- [X] Vérifier que les migrations s'exécutent sur une base vide

## 3. Backend : Utilisateurs et sécurité (minimum)

- [X] Entités `Utilisateur` et `Role`
- [X] Endpoint `POST /api/auth/login` : retourne un JWT
- [X] Filtre JWT et configuration Spring Security
- [X] Restriction des endpoints par rôle (`@PreAuthorize`)
- [X] Endpoint `GET /api/auth/me` : utilisateur connecté et ses rôles
- [X] Hachage des mots de passe (BCrypt)

## 4. Backend : Référentiels Département et Poste

### 4.1 Département

- [X] Entité `Departement` (id, nom, description)
- [X] DTO de requête et de réponse, mapper
- [X] Repository, service, controller
- [X] Endpoints :
  - `GET /api/departements`
  - `GET /api/departements/{id}`
  - `POST /api/departements` (RH, ADMIN)
  - `PUT /api/departements/{id}` (RH, ADMIN)
  - `DELETE /api/departements/{id}` (refus si des postes y sont rattachés)
- [X] Validation : nom obligatoire et unique

### 4.2 Poste

- [X] Entité `Poste` (id, titre, departement, description_profil)
- [X] DTO, mapper, repository, service, controller
- [X] Endpoints :
  - `GET /api/postes` (filtre optionnel par département)
  - `GET /api/postes/{id}`
  - `POST /api/postes` (RH, ADMIN)
  - `PUT /api/postes/{id}` (RH, ADMIN)
  - `DELETE /api/postes/{id}` (refus si utilisé par une demande)
- [X] Validation : titre obligatoire, département obligatoire et existant
- [X] Vérifier qu'aucune donnée "employé" n'est stockée dans le poste

## 5. Backend : DemandeRecrutement (création et consultation)

### 5.1 Règles de gestion à implémenter

- [ ] RG1 : seul un MANAGER peut créer une demande
- [ ] RG2 : champs obligatoires : poste, type, nombre de postes, motif, budget, date souhaitée
- [ ] RG3 : `nombre_postes` >= 1
- [ ] RG4 : `budget` > 0
- [ ] RG5 : `date_souhaitee` doit être postérieure à la date du jour
- [ ] RG6 : un manager ne peut créer une demande que pour un poste de son département
- [ ] RG7 : une demande en BROUILLON est modifiable et supprimable par son auteur uniquement
- [ ] RG8 : une fois soumise (EN_ATTENTE_RH), la demande n'est plus modifiable par le manager
- [ ] RG9 : le manager ne voit que ses demandes ; RH, FINANCE, DIRECTION voient toutes les demandes selon leur étape
- [ ] RG10 : la date de création et le demandeur sont renseignés automatiquement (traçabilité)

### 5.2 Implémentation

- [ ] Entité `DemandeRecrutement` avec énumérations Java `TypeDemande` et `StatutDemande`
- [ ] DTO : `DemandeCreateRequest`, `DemandeUpdateRequest`, `DemandeResponse`, `DemandeListItem`
- [ ] Repository avec recherche filtrée (statut, département, demandeur, période) et pagination
- [ ] Service `DemandeRecrutementService` : créer, modifier, supprimer, soumettre, consulter
- [ ] Machine à états simple : transitions autorisées (BROUILLON -> EN_ATTENTE_RH uniquement dans cette partie)
- [ ] Endpoints :
  - `POST /api/demandes` : créer en BROUILLON
  - `PUT /api/demandes/{id}` : modifier un brouillon
  - `DELETE /api/demandes/{id}` : supprimer un brouillon
  - `POST /api/demandes/{id}/soumettre` : passer à EN_ATTENTE_RH
  - `GET /api/demandes` : liste paginée et filtrée
  - `GET /api/demandes/{id}` : détail
- [ ] Gestion des erreurs métier avec messages explicites (code HTTP 400 / 403 / 404 / 409)

## 6. Frontend

### 6.1 Authentification

- [ ] Page de connexion
- [ ] Store Pinia `auth` (token, utilisateur, rôles)
- [ ] Garde de navigation (redirection si non connecté)
- [ ] Affichage du menu selon le rôle

### 6.2 Référentiels (RH, ADMIN)

- [ ] Page liste des départements + formulaire création / modification
- [ ] Page liste des postes (filtre par département) + formulaire création / modification
- [ ] Confirmation avant suppression, affichage des erreurs renvoyées par l'API

### 6.3 Demande de recrutement (Manager)

- [ ] Page "Mes demandes" : tableau paginé, filtres par statut, badge de statut en texte simple
- [ ] Formulaire de demande avec les champs : poste (liste liée au département), type (création / remplacement), nombre de postes, motif, budget, date souhaitée
- [ ] Validation côté client (mêmes règles que RG2 à RG5)
- [ ] Boutons : Enregistrer en brouillon, Soumettre, Supprimer (selon le statut)
- [ ] Page détail d'une demande (lecture seule après soumission)

### 6.4 Consultation (RH, Finance, Direction)

- [ ] Page liste de toutes les demandes avec filtres (statut, département, période)
- [ ] Page détail en lecture seule (les actions de validation seront ajoutées en partie 2)

## 7. Tests et qualité

- [ ] Tests unitaires des services (règles RG1 à RG10) avec JUnit 5 et Mockito
- [ ] Tests d'intégration des repositories avec Testcontainers (PostgreSQL)
- [ ] Tests des controllers avec MockMvc (codes HTTP, droits par rôle)
- [ ] Tests front des formulaires (Vitest) : facultatif pour la partie 1
- [ ] Jeu de test manuel via Swagger ou Postman, collection exportée dans le dépôt

## 8. Documentation et livrables

- [ ] README : prérequis, lancement base, back, front
- [ ] Schéma de la base (MCD / MLD) à jour
- [ ] Diagramme d'états de la demande de recrutement
- [ ] Liste des règles de gestion validée
- [ ] Documentation OpenAPI générée

## 9. Critères de fin de la partie 1

- [ ] Un manager se connecte, crée une demande, l'enregistre en brouillon, la modifie, puis la soumet
- [ ] Après soumission, la demande apparaît en EN_ATTENTE_RH côté RH
- [ ] Les référentiels Département et Poste sont gérables par la RH
- [ ] Les droits par rôle sont respectés
- [ ] Les migrations s'exécutent sur une base vide et les tests passent

---

## Partie 2 : Circuit de validation et ouverture du recrutement

Stack : Java 21 (Spring Boot) / Vue.js 3 / PostgreSQL

Prérequis : partie 1 terminée (référentiels, utilisateurs et rôles, demande créée et soumise en EN_ATTENTE_RH).

Périmètre de la partie 2 :

- Workflow de validation : Manager -> RH -> (Finance) -> Direction
- Traçabilité de chaque décision (table `validation_demande`)
- Notifications simples entre les acteurs
- Passage de la demande validée à l'ouverture du recrutement (point d'entrée du bloc 2)

Légende : [ ] à faire, [x] terminé

---

## 0. Décisions de conception (avant de coder)

Écarts constatés entre le schéma de flux (image) et la leçon :

- La leçon prévoit : Manager -> RH -> (Finance) -> Direction -> ouverture du recrutement.
- Le schéma montre : Manager -> RH -> Finance -> retour au Manager -> demande validée -> RH -> commencer recrutement. L'étape Direction n'y apparaît pas.

Décisions à trancher et à consigner dans le README :

- [ ] Direction : étape obligatoire ou non ? Proposition : obligatoire (conforme à la leçon), le schéma de flux est à compléter
- [ ] Finance : étape facultative, déclenchée uniquement si la RH choisit "demande d'analyse budget" (conforme à "(Finance)" dans la leçon)
- [ ] Retour au Manager après Finance : simple notification d'information, ou confirmation bloquante ? Proposition : notification seulement
- [ ] Rejet : définitif, ou retour au Manager pour correction ? Proposition : deux issues distinctes, REJETE (définitif) et A_CORRIGER (retour au manager)
- [ ] Une Direction peut-elle valider sans passage par la Finance ? Proposition : oui
- [ ] Qui ouvre le recrutement : la RH, après validation finale (conforme au schéma)

Machine à états retenue :

```
BROUILLON -> EN_ATTENTE_RH
EN_ATTENTE_RH -> ANALYSE_FINANCE        (RH : accepte et demande l'analyse budget)
EN_ATTENTE_RH -> EN_ATTENTE_DIRECTION   (RH : accepte sans analyse)
EN_ATTENTE_RH -> A_CORRIGER             (RH : demande une correction)
EN_ATTENTE_RH -> REJETE                 (RH : rejet définitif)
ANALYSE_FINANCE -> EN_ATTENTE_DIRECTION (Finance : budget accepté)
ANALYSE_FINANCE -> A_CORRIGER           (Finance : budget à revoir)
ANALYSE_FINANCE -> REJETE               (Finance : rejet)
EN_ATTENTE_DIRECTION -> VALIDE          (Direction : accepte)
EN_ATTENTE_DIRECTION -> A_CORRIGER      (Direction : demande une correction)
EN_ATTENTE_DIRECTION -> REJETE          (Direction : rejet)
A_CORRIGER -> EN_ATTENTE_RH             (Manager : corrige et resoumet)
VALIDE -> RECRUTEMENT_OUVERT            (RH : lance le recrutement)
```

- [ ] Valider ce diagramme avec l'équipe
- [ ] Mettre à jour le schéma de flux (ajouter Direction et le cas A_CORRIGER)

## 1. Base de données

- [ ] Migration `V7__alter_demande_statuts.sql` : ajouter les statuts A_CORRIGER, EN_ATTENTE_DIRECTION, RECRUTEMENT_OUVERT (contrainte `CHECK` ou type énuméré)
- [ ] Contrainte `CHECK` sur `validation_demande.etape` : RH, FINANCE, DIRECTION
- [ ] Contrainte `CHECK` sur `validation_demande.statut` : ACCEPTE, REJETE, DEMANDE_ANALYSE, A_CORRIGER
- [ ] Ajouter `validation_demande.budget_valide NUMERIC(12,2)` (montant retenu par la Finance, facultatif)
- [ ] Ajouter `demande_recrutement.cycle INT DEFAULT 1` : numéro du cycle de validation, incrémenté à chaque resoumission après correction (permet de distinguer les historiques)
- [ ] Ajouter `validation_demande.cycle INT` pour rattacher chaque décision à son cycle
- [ ] Ajouter `demande_recrutement.version INT` pour le verrouillage optimiste
- [ ] Ajouter `demande_recrutement.date_ouverture_recrutement TIMESTAMP` et `ouvert_par_id`
- [ ] Rendre `validation_demande` en ajout seul : aucune modification ni suppression (voir section 3)
- [ ] Index sur `validation_demande(demande_recrutement_id, cycle)` et sur `demande_recrutement.statut`
- [ ] Créer la table `notification` (id, destinataire_id, message, lien, lue, date_creation)
- [ ] Mettre à jour le jeu de données de test (une demande à chaque étape)

## 2. Backend : moteur de workflow

- [ ] Créer un package `workflow` indépendant des controllers
- [ ] Énumération `EtapeValidation` (RH, FINANCE, DIRECTION)
- [ ] Énumération `DecisionValidation` (ACCEPTE, REJETE, DEMANDE_ANALYSE, A_CORRIGER)
- [ ] Classe `TransitionRegistry` : table des transitions autorisées (statut courant, rôle, décision) -> statut suivant
- [ ] Méthode `appliquerDecision(demande, utilisateur, decision, commentaire)` :
  - vérifie que le statut courant correspond à l'étape de l'utilisateur
  - vérifie le rôle de l'utilisateur
  - applique la transition
  - enregistre la ligne de validation
  - déclenche la notification
  - le tout dans une seule transaction
- [ ] Exception métier `TransitionInterditeException` (code HTTP 409)
- [ ] Prévoir une structure extensible (les mêmes mécanismes serviront aux workflows des blocs 2, 3 et 4)

## 3. Backend : traçabilité des validations

- [ ] Entité `ValidationDemande` (demande, étape, validateur, statut, commentaire, budget_valide, cycle, date_validation)
- [ ] Repository : historique d'une demande trié par date
- [ ] Aucun endpoint de modification ou suppression sur cette table
- [ ] Enregistrer l'identité du validateur depuis le contexte de sécurité, jamais depuis le corps de la requête
- [ ] Endpoint `GET /api/demandes/{id}/historique` : liste chronologique des décisions
- [ ] Ajouter l'historique complet dans le détail de la demande

## 4. Backend : actions de validation

### 4.1 Règles de gestion

- [ ] RG11 : seule la RH peut agir sur une demande EN_ATTENTE_RH
- [ ] RG12 : seule la Finance peut agir sur une demande ANALYSE_FINANCE
- [ ] RG13 : seule la Direction peut agir sur une demande EN_ATTENTE_DIRECTION
- [ ] RG14 : un commentaire est obligatoire pour REJETE et A_CORRIGER
- [ ] RG15 : la Finance doit pouvoir renseigner un budget validé ; s'il diffère du budget demandé, un commentaire est obligatoire
- [ ] RG16 : une décision est définitive ; pas d'annulation, seule une nouvelle décision à l'étape suivante est possible
- [ ] RG17 : un validateur ne peut pas valider une demande qu'il a lui-même créée
- [ ] RG18 : une demande REJETE ou RECRUTEMENT_OUVERT ne peut plus changer de statut
- [ ] RG19 : le Manager ne peut resoumettre qu'une demande A_CORRIGER dont il est l'auteur ; la resoumission incrémente le cycle
- [ ] RG20 : deux décisions simultanées sur la même demande sont impossibles (verrouillage optimiste, erreur 409)

### 4.2 Endpoints

- [ ] `POST /api/demandes/{id}/valider-rh` : corps `{ decision, commentaire, analyseBudget }`
- [ ] `POST /api/demandes/{id}/valider-finance` : corps `{ decision, commentaire, budgetValide }`
- [ ] `POST /api/demandes/{id}/valider-direction` : corps `{ decision, commentaire }`
- [ ] `POST /api/demandes/{id}/resoumettre` (Manager, après A_CORRIGER)
- [ ] `POST /api/demandes/{id}/ouvrir-recrutement` (RH, statut VALIDE uniquement)
- [ ] `GET /api/demandes/a-traiter` : demandes en attente pour le rôle de l'utilisateur connecté
- [ ] Restriction des endpoints par rôle (`@PreAuthorize`)
- [ ] Documentation OpenAPI de chaque endpoint (codes d'erreur compris)

## 5. Backend : ouverture du recrutement

- [ ] Service `OuvrirRecrutement` : vérifie le statut VALIDE, passe à RECRUTEMENT_OUVERT, renseigne date et auteur
- [ ] Publier un événement applicatif `RecrutementOuvertEvent` (écouté plus tard par le bloc 2 pour préparer l'offre d'emploi)
- [ ] Ne pas créer l'offre d'emploi ici : c'est l'objet du bloc 2
- [ ] Notifier le Manager demandeur

## 6. Backend : notifications

- [ ] Entité, repository et service `NotificationService`
- [ ] Notifications générées sur chaque transition :
  - soumission : à la RH
  - passage en analyse : à la Finance
  - passage en attente Direction : à la Direction
  - A_CORRIGER, REJETE, VALIDE, RECRUTEMENT_OUVERT : au Manager
  - retour d'analyse budget : au Manager (information, conforme au schéma)
- [ ] Endpoints :
  - `GET /api/notifications` (non lues d'abord)
  - `PATCH /api/notifications/{id}/lue`
  - `GET /api/notifications/non-lues/count`
- [ ] Envoi d'email : facultatif, à reporter si le temps manque

## 7. Frontend

### 7.1 Écrans de traitement

- [ ] Page "À traiter" selon le rôle (RH, Finance, Direction) : tableau des demandes en attente
- [ ] Page détail enrichie : informations de la demande, historique des décisions, zone d'action
- [ ] Composant `ActionValidation` : boutons selon le rôle et le statut (Accepter, Demander une correction, Rejeter), champ commentaire
- [ ] Case "Demander l'analyse budget" visible pour la RH
- [ ] Champ "Budget validé" visible pour la Finance
- [ ] Confirmation avant toute décision définitive
- [ ] Bouton "Ouvrir le recrutement" visible pour la RH sur une demande VALIDE

### 7.2 Suivi

- [ ] Composant `TimelineValidation` : frise chronologique de l'historique (étape, validateur, décision, commentaire, date)
- [ ] Composant `StatutDemande` : libellé textuel du statut, sans couleur imposée
- [ ] Indicateur de progression : RH, Finance, Direction, Recrutement
- [ ] Côté Manager : bouton "Corriger et resoumettre" sur une demande A_CORRIGER, avec affichage du commentaire de correction

### 7.3 Notifications

- [ ] Cloche dans l'en-tête avec compteur de notifications non lues
- [ ] Liste des notifications avec lien vers la demande
- [ ] Rafraîchissement périodique (polling toutes les 30 à 60 secondes)

### 7.4 Tableau de bord simple

- [ ] Compteurs par statut (visible RH et Direction)
- [ ] Liste des 10 dernières demandes

## 8. Tests et qualité

- [ ] Tests unitaires de `TransitionRegistry` : toutes les transitions autorisées et interdites
- [ ] Tests unitaires des règles RG11 à RG20
- [ ] Test de bout en bout du circuit complet : soumission, RH, Finance, Direction, ouverture
- [ ] Test du circuit sans Finance
- [ ] Test du circuit avec correction : A_CORRIGER, resoumission, nouveau cycle
- [ ] Test du rejet définitif
- [ ] Test de concurrence : deux validations simultanées (une seule doit aboutir)
- [ ] Test d'accès : un rôle non autorisé reçoit 403
- [ ] Vérifier que l'historique n'est jamais modifiable
- [ ] Jeu de test manuel : collection Postman mise à jour

## 9. Documentation et livrables

- [ ] Diagramme d'états final de la demande de recrutement
- [ ] Diagramme de séquence : soumission jusqu'à l'ouverture du recrutement
- [ ] Tableau des droits : action x rôle x statut
- [ ] Liste des règles de gestion RG1 à RG20 consolidée
- [ ] Schéma de flux (image) mis à jour avec Direction et A_CORRIGER
- [ ] README et documentation OpenAPI à jour

## 10. Critères de fin de la partie 2

- [ ] Une demande soumise parcourt RH, Finance (facultative), Direction et atteint VALIDE
- [ ] La RH peut ouvrir le recrutement sur une demande VALIDE
- [ ] Chaque décision est tracée (qui, quand, quoi, pourquoi) et non modifiable
- [ ] Les cas de rejet et de correction fonctionnent, avec nouveau cycle après resoumission
- [ ] Les notifications arrivent aux bons acteurs
- [ ] Les transitions interdites et les accès non autorisés sont refusés
- [ ] Les tests passent et la documentation est à jour

---

## Ordre d'exécution recommandé

1. Section 0 (trancher les décisions, valider la machine à états)
2. Section 1 (migrations)
3. Sections 2 et 3 (moteur de workflow et traçabilité) avec leurs tests unitaires
4. Section 4 (actions de validation), puis 7.1 et 7.2 (front correspondant)
5. Section 5 (ouverture du recrutement)
6. Section 6 (notifications), puis 7.3
7. Section 7.4 (tableau de bord)
8. Sections 8, 9, 10 (tests de bout en bout, documentation, validation)
