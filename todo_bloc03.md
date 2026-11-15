
# TODO - SIRH - Bloc 3 : Sélection et embauche

Stack : Java 21 (Spring Boot) / Vue.js 3 / PostgreSQL

Prérequis : bloc 2 terminé (candidatures au statut RETENU, événement `CandidatRetenuEvent`).

Périmètre : proposition d'embauche -> négociation -> acceptation -> création du dossier salarié (matricule) -> contrat -> signature et validation -> affectation.
Hors périmètre : checklist d'onboarding (bloc 4).

Légende : [ ] à faire, [x] terminé

---

## 0. Décisions de conception

- [ ] Règle centrale : CANDIDAT != EMPLOYE. L'employé n'est créé qu'après acceptation, et reste lié au candidat d'origine
- [ ] Négociation : le schéma de flux montre plusieurs allers-retours (accepter tout, accepter avec remarque, ajuster et renvoyer, négocier un autre type de contrat). Proposition : une proposition est versionnée, chaque tour crée une nouvelle ligne, un nombre maximum de tours est fixé (ex. 3)
- [ ] Qui renseigne quoi : la RH propose (date, lieu, salaire, type de contrat) ; le candidat accepte, refuse ou répond avec une remarque et ses souhaits
- [ ] Contrats autorisés : CE, CDD, CDI. Règles (leçon) : CE 6 mois renouvelable, CDD 2 ans renouvelable 1 fois, CDI sans date de fin. À confirmer par la recherche sur le contrat de travail (consigne de la leçon)
- [ ] Machine à états de la proposition :

```
EN_ATTENTE -> ACCEPTE | REFUSE | NEGOCIATION
NEGOCIATION -> (RH) NOUVELLE_VERSION en EN_ATTENTE | REFUSE
ACCEPTE -> création de l'employé et du contrat
```

- [ ] Machine à états du contrat : BROUILLON -> CREE -> SIGNE -> VALIDE (le script SQL mélange BROUILLON et CREER/SIGNER/VALIDE : à unifier)
- [ ] Création du compte utilisateur du nouvel employé : ici (schéma de flux : "Créer compte pour le nouveau") ou au bloc 4 ? Proposition : à la validation du contrat

## 1. Base de données

- [ ] Corriger le statut du contrat : un seul vocabulaire (BROUILLON, CREE, SIGNE, VALIDE) avec `CHECK`
- [ ] `proposition_embauche` : ajouter `version INT`, `proposition_precedente_id`, `emise_par_id`, `date_reponse`; remarque du candidat conservée à chaque version (historique de négociation, pas d'écrasement)
- [ ] `proposition_embauche` : `UNIQUE (candidature_id, version)` et une seule proposition active par candidature
- [ ] `employe` : `UNIQUE (candidat_id)` ; ne pas dupliquer nom, prénom, téléphone sans raison (les lire depuis `candidat`, ou les copier volontairement et le documenter)
- [ ] `employe.matricule` : généré par séquence PostgreSQL (format ex. `EMP-2026-0001`), jamais saisi à la main
- [ ] `contrat` : `CHECK` sur le type, `date_fin` obligatoire pour CE et CDD, nulle pour CDI, `date_fin > date_debut`
- [ ] `contrat` : ajouter `numero_renouvellement INT DEFAULT 0` et `contrat_precedent_id` (chaînage des contrats successifs)
- [ ] `affectation` : ajouter `date_fin` (nullable) ; `departement_id` est déductible de `poste` : le garder seulement pour l'historique, ou le supprimer
- [ ] Index : `contrat(employe_id, statut)`, `affectation(employe_id, date_fin)`, `proposition_embauche(candidature_id)`
- [ ] Migrations Flyway et jeu de test (une proposition par statut)

## 2. Backend : proposition d'embauche

- [ ] Entité `PropositionEmbauche` et DTO
- [ ] Créer la proposition depuis un candidat RETENU : `POST /api/candidatures/{id}/propositions` (date de début, lieu, salaire, type de contrat)
- [ ] Réponse du candidat : `POST /api/propositions/{id}/repondre` avec `ACCEPTER`, `REFUSER` ou `NEGOCIER` (+ remarque, souhaits)
- [ ] Réponse de la RH à une négociation : `POST /api/propositions/{id}/ajuster` (crée la version suivante) ou `POST /api/propositions/{id}/rejeter`
- [ ] Accès du candidat : lien sécurisé à usage limité (jeton par email), sans compte
- [ ] Règles :
  - proposition possible uniquement pour une candidature RETENU
  - budget : le salaire proposé ne dépasse pas le budget de la demande (alerte ou blocage à décider)
  - expiration de la proposition après N jours sans réponse
  - nombre maximal de tours de négociation
- [ ] Historique complet des versions consultable (`GET /api/candidatures/{id}/propositions`)

## 3. Backend : création de l'employé

- [ ] À l'acceptation, dans une seule transaction : créer `employe` (statut EN_ONBOARDING), générer le matricule, créer le contrat en BROUILLON, créer l'affectation (poste de la demande, date de début)
- [ ] Idempotence : une proposition acceptée ne crée jamais deux employés
- [ ] Si le candidat est déjà employé (ancien salarié) : réutiliser le dossier ou refuser, règle à décider
- [ ] Mettre à jour la candidature (statut EMBAUCHE) et clôturer l'offre si le quota de postes est atteint
- [ ] Événement `EmployeCreeEvent` pour le bloc 4 (génération de la checklist d'onboarding)

## 4. Backend : contrat

- [ ] Entité `Contrat`, service et DTO
- [ ] Transitions : `creer` (BROUILLON -> CREE), `signer` (CREE -> SIGNE, RH et candidat), `valider` (SIGNE -> VALIDE, RH)
- [ ] Génération du document PDF du contrat à partir d'un modèle (date, lieu, salaire, type) ; stockage via `contrat_url`
- [ ] Dépôt du contrat signé (scan) avec contrôle du type et de la taille de fichier
- [ ] Règles de durée : CE 6 mois renouvelable, CDD 2 ans renouvelable une seule fois, CDI sans fin ; refus de toute création non conforme
- [ ] Un seul contrat actif par employé à une date donnée (pas de chevauchement)
- [ ] Un contrat VALIDE n'est plus modifiable ; toute évolution passe par un nouveau contrat chaîné
- [ ] Endpoints : `GET /api/employes/{id}/contrats`, `POST /api/contrats/{id}/signer`, `POST /api/contrats/{id}/valider`

## 5. Backend : affectation et dossier employé

- [ ] Entité `Affectation` ; création à partir du poste de la demande
- [ ] Un employé a au plus une affectation en cours (`date_fin` nulle)
- [ ] Endpoint `GET /api/employes/{id}` : dossier (identité, matricule, contrats, affectation courante, historique)
- [ ] Endpoint `GET /api/employes` avec filtres (statut, département, recherche par nom ou matricule)
- [ ] Création du compte utilisateur à la validation du contrat, avec rôle EMPLOYE et mot de passe temporaire à changer
- [ ] Notifications : proposition envoyée, réponse reçue (RH), contrat prêt à signer, contrat validé

## 6. Frontend

### 6.1 Espace candidat (lien sécurisé)

- [ ] Page de consultation de la proposition : date, lieu, salaire, type de contrat
- [ ] Boutons Accepter, Refuser, Négocier (formulaire de remarque et souhaits)
- [ ] Suivi des échanges (versions précédentes)

### 6.2 Espace RH

- [ ] Liste des candidats retenus avec action "Envoyer une proposition"
- [ ] Formulaire de proposition ; vue de négociation avec historique des versions et actions Ajuster / Rejeter
- [ ] Page de suivi des propositions par statut
- [ ] Fiche contrat : création, impression PDF, dépôt du signé, validation
- [ ] Liste et fiche employé (matricule, contrats, affectation)

## 7. Tests

- [ ] Unitaires : transitions de proposition et de contrat, règles de durée CE / CDD / CDI, génération du matricule
- [ ] Intégration : acceptation qui crée employé + contrat + affectation en une transaction, et annulation complète en cas d'erreur
- [ ] Idempotence : double acceptation sans doublon
- [ ] Négociation : plusieurs tours, dépassement du maximum, expiration
- [ ] Bout en bout : retenu -> proposition -> négociation -> acceptation -> contrat signé et validé
- [ ] Sécurité : jeton candidat invalide ou expiré, accès par rôle

## 8. Documentation

- [ ] Diagrammes d'états : proposition et contrat
- [ ] Règles de gestion du bloc 3 (suite de la numérotation)
- [ ] Note de synthèse sur les types de contrat et leurs durées (recherche demandée par la leçon)
- [ ] Schéma de base et OpenAPI à jour

## 9. Critères de fin

- [ ] Un candidat retenu reçoit une proposition, peut accepter, refuser ou négocier sur plusieurs tours
- [ ] L'acceptation crée en une opération le dossier employé, le matricule, le contrat et l'affectation
- [ ] Le contrat suit les états créé, signé, validé, avec le document conservé
- [ ] Les règles de durée CE, CDD, CDI sont appliquées
- [ ] L'historique de négociation est conservé et non écrasé
- [ ] Les tests passent, la documentation est à jour

---

## Ordre d'exécution recommandé

1. Sections 0 et 1 (décisions, migrations)
2. Section 2 (proposition), puis 6.1 et 6.2 partie propositions
3. Sections 3 et 4 (création de l'employé, contrat), puis 6.2 partie contrats
4. Section 5 (affectation, dossier, compte)
5. Sections 7, 8, 9
