# TODO - SIRH - Bloc 2 : Recrutement

Stack : Java 21 (Spring Boot) / Vue.js 3 / PostgreSQL

Prérequis : bloc 1 terminé (demande au statut RECRUTEMENT_OUVERT, événement `RecrutementOuvertEvent`).

Périmètre : demande validée -> offre d'emploi -> candidatures -> présélection -> tests et entretiens -> liste des candidats retenus.
Hors périmètre : proposition d'embauche et contrat (bloc 3).

Légende : [ ] à faire, [x] terminé

---

## 0. Décisions de conception

- [ ] Accès candidat : page publique de candidature sans compte, ou compte candidat ? Proposition : formulaire public sans compte (recherche du candidat par email)
- [ ] Traçabilité : chaque candidature conserve une copie figée du CV au moment du dépôt (le CV du candidat peut changer ensuite)
- [ ] Séparer clairement Test et Entretien (le schéma de flux les distingue) : un seul modèle `evaluation` avec un type, ou deux tables ? Proposition : un modèle unique avec `type_evaluation`
- [ ] Une offre peut-elle correspondre à plusieurs postes (`nombre_postes` de la demande) ? Proposition : oui, la clôture est manuelle ou automatique quand le quota de retenus est atteint
- [ ] Machine à états de la candidature :

```
RECU -> PRESELECTIONNE | EN_ATTENTE | REJETE
EN_ATTENTE -> PRESELECTIONNE | REJETE        (après les acceptés)
PRESELECTIONNE -> TEST_PLANIFIE -> TEST_PASSE -> ENTRETIEN_PLANIFIE -> ENTRETIEN_PASSE
TEST_PASSE | ENTRETIEN_PASSE -> REJETE
ENTRETIEN_PASSE -> RETENU
```

- [ ] Offre : BROUILLON -> PUBLIEE -> CLOTUREE

## 1. Base de données

- [ ] Corriger `bloc-02.sql` : virgule en trop à la fin de la table `candidat`, faute `evalutateur_id` -> `evaluateur_id`
- [ ] `offre_emploi.statut` : défaut `BROUILLON` (le script met `OUVERTE`, incohérent avec le commentaire)
- [ ] Ajouter `UNIQUE (offre_emploi_id, candidat_id)` sur `candidature`
- [ ] Ajouter `candidature_historique` (candidature_id, ancien_statut, nouveau_statut, utilisateur_id, commentaire, date) : traçabilité, ajout seul
- [ ] Ajouter `candidature.cv_snapshot JSONB` : copie figée du CV (profil, expériences, formations) à la date de candidature
- [ ] Ajouter `candidature.rang_attente INT` pour la liste d'attente
- [ ] Contraintes `CHECK` sur les statuts, `note` entre 0 et 20, `date_cloture >= date_publication`
- [ ] Index sur `candidature(offre_emploi_id, statut)` et `offre_emploi.statut`
- [ ] Migrations Flyway `V8` et suivantes ; jeu de test (1 offre, 6 candidats à différents statuts)

## 2. Backend : offre d'emploi

- [ ] Entité `OffreEmploi` et DTO
- [ ] Écouter `RecrutementOuvertEvent` : création automatique d'une offre BROUILLON préremplie (titre et profil du poste, lieu)
- [ ] RH : modifier le brouillon, publier, clôturer
- [ ] Endpoints : `GET /api/offres`, `GET /api/offres/{id}`, `PUT /api/offres/{id}`, `POST /api/offres/{id}/publier`, `POST /api/offres/{id}/cloturer`
- [ ] Endpoint public `GET /api/public/offres` (offres PUBLIEE uniquement)
- [ ] Partage et impression (schéma de flux) : lien public de l'offre + page imprimable ou export PDF
- [ ] Règles : une offre ne se publie que si la demande est RECRUTEMENT_OUVERT ; une offre clôturée n'accepte plus de candidature

## 3. Backend : candidat, CV et candidature

- [ ] Entités `Candidat`, `CvCandidature`, `ExperienceProfessionnelle`, `FormationCandidat`
- [ ] Endpoint public `POST /api/public/offres/{id}/candidatures` : crée ou retrouve le candidat (par email), enregistre le CV structuré, génère le `cv_snapshot`, statut RECU
- [ ] Upload du CV PDF et de la lettre de motivation (stockage fichier ou GED, seul l'URL en base) ; limites de type et de taille
- [ ] Règles :
  - une personne ne postule qu'une fois à une même offre
  - candidature refusée si l'offre n'est pas PUBLIEE
  - un candidat peut avoir plusieurs candidatures sur des offres différentes (CANDIDAT != EMPLOYE)
- [ ] Endpoints RH : `GET /api/offres/{id}/candidatures` (filtre statut, pagination), `GET /api/candidatures/{id}` (détail + historique)

## 4. Backend : sélection de dossiers

- [ ] Endpoint `POST /api/candidatures/{id}/decision-dossier` : `ACCEPTER`, `LISTE_ATTENTE`, `REJETER` + commentaire
- [ ] Liste d'attente ordonnée ; promotion d'un candidat en attente vers PRESELECTIONNE (schéma : "après les accepter")
- [ ] Rejet : commentaire obligatoire
- [ ] Chaque changement écrit une ligne dans `candidature_historique` (qui, quand, pourquoi)

## 5. Backend : tests et entretiens

- [ ] Entité `Evaluation` (candidature, type : TEST_TECHNIQUE / ENTRETIEN_RH / ENTRETIEN_MANAGER, date, évaluateur, note, remarque, résultat)
- [ ] Planification : `POST /api/candidatures/{id}/evaluations` (type, date, évaluateur)
- [ ] Saisie du résultat : `PUT /api/evaluations/{id}/resultat` (date, note, remarque, résultat ACCEPTE / LISTE_D_ATTENTE / REJETE), conforme au formulaire du schéma (date, remarque)
- [ ] Règles :
  - un test ne se planifie que pour une candidature PRESELECTIONNE
  - un entretien ne se planifie qu'après un test accepté
  - pas de chevauchement de dates pour un même évaluateur
  - un résultat saisi n'est plus modifiable (correction par une nouvelle ligne)
- [ ] Décision finale : `POST /api/candidatures/{id}/decision-finale` (RETENU ou REJETE, commentaire) ; la décision finale est tracée
- [ ] Endpoint `GET /api/offres/{id}/candidats-retenus` : alimente l'envoi des propositions d'embauche (bloc 3)

## 6. Backend : clôture et transversal

- [ ] Clôture du recrutement : manuelle par la RH, ou automatique quand le nombre de retenus atteint `nombre_postes`
- [ ] À la clôture : candidatures en cours -> REJETE ou EN_ATTENTE avec notification
- [ ] Événement `CandidatRetenuEvent` pour le bloc 3
- [ ] Notifications : nouvelle candidature (RH), planification d'évaluation (évaluateur), résultat saisi (RH)
- [ ] Rôles : RH (tout), MANAGER (consultation et évaluations de ses offres), accès public limité aux endpoints `/api/public/**`

## 7. Frontend

### 7.1 Espace public candidat

- [ ] Liste des offres publiées et page détail d'une offre
- [ ] Formulaire de candidature en plusieurs étapes : identité, profil et compétences, expériences (ajout multiple), formations (ajout multiple), pièces jointes
- [ ] Message de confirmation après dépôt

### 7.2 Espace RH et Manager

- [ ] Liste des offres et formulaire d'édition ; boutons Publier, Partager (copier le lien), Imprimer, Clôturer
- [ ] Liste des candidatures d'une offre avec filtres par statut ; vue en colonnes par étape (facultatif)
- [ ] Détail candidature : CV figé, pièces jointes, historique chronologique, actions selon le statut
- [ ] Actions : Accepter / Liste d'attente / Rejeter, Planifier test ou entretien, Saisir résultat, Décision finale
- [ ] Calendrier ou liste des évaluations planifiées
- [ ] Page "Candidats retenus" par offre

## 8. Tests

- [ ] Unitaires : transitions de statut de la candidature, règles de planification, quota de retenus
- [ ] Intégration : candidature publique (doublon, offre clôturée, fichier trop gros)
- [ ] Bout en bout : offre -> candidature -> présélection -> test -> entretien -> retenu
- [ ] Cas : liste d'attente puis promotion, rejet avec commentaire, clôture automatique
- [ ] Sécurité : endpoints publics limités, un rôle non autorisé reçoit 403

## 9. Documentation

- [ ] Diagrammes d'états : offre et candidature
- [ ] Règles de gestion du bloc 2 numérotées (suite de RG20)
- [ ] Schéma de base et OpenAPI à jour

## 10. Critères de fin

- [ ] Une demande validée génère une offre, publiée puis partageable
- [ ] Un candidat externe dépose une candidature complète sans compte
- [ ] La RH présélectionne, planifie tests et entretiens, saisit les résultats et décide
- [ ] Tout changement de statut est tracé et le CV d'origine est conservé tel que déposé
- [ ] La liste des candidats retenus est disponible pour le bloc 3
- [ ] Tests passent, documentation à jour

---

## Ordre d'exécution recommandé

1. Sections 0 et 1 (décisions, migrations)
2. Section 2 (offre), puis 7.2 partie offres
3. Section 3 (candidature publique), puis 7.1
4. Sections 4 et 5 (sélection, évaluations), puis 7.2 partie candidatures
5. Section 6 (clôture, événements, notifications)
6. Sections 8, 9, 10
