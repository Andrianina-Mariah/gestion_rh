## Installation

### Base de données

Si Docker est disponible, depuis ce dossier :

```bash
docker compose up -d postgres
```

Avec une installation PostgreSQL locale, l'application utilise
`localhost:5432`, le port PostgreSQL standard.

Sans Docker, créez une base `sirh` et l'utilisateur `sirh_user` avec le mot de
passe `sirh`.

### Démarrer le backend

```bash
mvn spring-boot:run
```

Le backend écoute ensuite sur `http://localhost:8080`.

Les migrations Flyway, dont `V6__seed_data.sql`, sont chargées depuis :

```text
src/main/resources/db/migration/
```

## Partie 2 : décisions du circuit de validation

Le workflow implémenté par `V7__validation_workflow.sql` retient les décisions
suivantes :

- La Direction est une étape obligatoire après la décision RH ou Finance.
- La Finance est facultative et n'intervient que lorsque la RH demande une
  analyse budgétaire.
- Le retour au Manager après la décision Finance est une notification
  d'information, sans confirmation bloquante.
- `REJETE` est définitif. `A_CORRIGER` renvoie la demande au Manager, qui peut
  la corriger et la resoumettre.
- La Direction peut valider directement après la RH, sans passage par la
  Finance.
- La RH ouvre le recrutement après la validation finale de la Direction.

Les décisions sont conservées dans `validation_demande` en ajout seul. Chaque
décision est rattachée au `cycle` de validation de la demande. Les notifications
simples sont conservées dans `notification`.

### Endpoint de décision

Les rôles RH, Finance et Direction peuvent appeler :

```http
POST /api/demandes/{id}/validation
Content-Type: application/json
Authorization: Bearer <token>
```

Exemple :

```json
{
  "decision": "DEMANDE_ANALYSE",
  "commentaire": "Le budget doit être vérifié par Finance."
}
```

Une transition impossible retourne HTTP `409` avec l'erreur
`Transition interdite`. La décision, la transition de statut et les
notifications sont enregistrées dans une seule transaction.

### Ouverture du recrutement et notifications

`OuvrirRecrutementService` passe une demande `VALIDE` à
`RECRUTEMENT_OUVERT`, renseigne la date et l'utilisateur RH, notifie le
Manager demandeur et publie `RecrutementOuvertEvent`. Aucun objet d'offre
d'emploi n'est créé à ce stade : il sera préparé par le bloc 2.

Les notifications sont accessibles via :

```http
GET /api/notifications
GET /api/notifications/non-lues/count
PATCH /api/notifications/{id}/lue
```

La liste place les notifications non lues en premier. Chaque utilisateur ne
peut lire et marquer comme lue que ses propres notifications.

## Bloc 2 : recrutement

La migration `V8__create_recruitment_block.sql` est la source de vérité pour
les offres, les candidats, les CV structurés, les candidatures et les
évaluations. Le script `database/bloc-02.sql` est conservé comme document de
référence et n'est pas exécuté par Flyway.

La migration `V9__protect_closed_offers.sql` empêche également au niveau
PostgreSQL le dépôt d'une candidature sur une offre qui n'est pas `PUBLIEE`,
notamment lorsqu'elle est `CLOTUREE`.

La migration `V10__align_recruitment_foreign_keys.sql` aligne les clés
étrangères du bloc 2 en `BIGINT`, conformément aux identifiants `BIGSERIAL`
des candidats, CV, offres et candidatures.

### Sélection des dossiers

La RH peut décider du dossier d'une candidature via :

```http
POST /api/candidatures/{id}/decision-dossier
```

avec une décision `ACCEPTER`, `LISTE_ATTENTE` ou `REJETER`. Une décision
`REJETER` exige un commentaire. Les dossiers acceptés passent à
`PRESELECTIONNE`, ceux placés en attente reçoivent un `rang_attente`
séquentiel, et chaque décision est ajoutée à `candidature_historique`.

La liste d'attente est disponible dans l'ordre de son rang :

```http
GET /api/offres/{id}/candidatures/liste-attente
```

La RH peut promouvoir un dossier en attente avec :

```http
POST /api/candidatures/{id}/promouvoir
```

### Tests et entretiens

Une évaluation est planifiée par la RH ou un administrateur avec :

```http
POST /api/candidatures/{id}/evaluations
```

Les types sont `TEST_TECHNIQUE`, `ENTRETIEN_RH` et `ENTRETIEN_MANAGER`.
Un test exige une candidature `PRESELECTIONNEE`; un entretien exige un test
accepté et une candidature `TEST_PASSE`.

Le résultat est saisi une seule fois :

```http
PUT /api/evaluations/{id}/resultat
```

Les résultats possibles sont `ACCEPTE`, `LISTE_ATTENTE` et `REJETE`. Une
évaluation déjà renseignée est immuable ; une correction doit être portée par
une nouvelle évaluation. Un même évaluateur ne peut pas avoir deux
évaluations planifiées au même horaire.

La décision finale est disponible avec :

```http
POST /api/candidatures/{id}/decision-finale
GET /api/offres/{id}/candidats-retenus
```

### Clôture et accès transversaux

La RH clôture manuellement une offre publiée avec :

```http
POST /api/offres/{id}/cloturer
```

À la clôture, les candidatures encore en cours passent à `REJETE` et une
entrée d'historique ainsi qu'une notification sont créées. Les candidatures
`EN_ATTENTE` restent dans la liste d'attente et reçoivent une notification.
La clôture automatique est déclenchée lorsqu'une décision finale fait
atteindre le quota `nombre_postes` de candidats `RETENU`.

Chaque candidat retenu publie un `CandidatRetenuEvent` pour le bloc 3. Les
notifications couvrent aussi les nouvelles candidatures, les évaluations
planifiées et les résultats saisis.

La RH et l'ADMIN disposent de la gestion complète. Un `MANAGER` peut
consulter ses offres, leurs candidatures, les évaluations et les candidats
retenus, et saisir les évaluations de ses propres offres. Les contrôles
métier vérifient systématiquement que l'offre appartient au Manager connecté.
Les endpoints publics restent limités à `/api/public/**`.

## Bloc 3 : sélection et embauche — base de données

La migration `V12__create_hiring_block.sql` applique les décisions du Bloc 3 :

- les propositions sont versionnées et chaînées par
  `proposition_precedente_id`, avec au plus une version active
  (`EN_ATTENTE` ou `NEGOCIATION`) par candidature ;
- le candidat reste distinct de l'employé, et `employe.candidat_id` est
  unique ;
- le matricule est généré par PostgreSQL au format
  `EMP-AAAA-0001` et ne peut pas être saisi manuellement ;
- les contrats autorisent uniquement `CE`, `CDD` et `CDI`, avec les statuts
  `BROUILLON`, `CREE`, `SIGNE`, `VALIDE` ;
- `date_fin` est obligatoire pour `CE` et `CDD`, nulle pour `CDI`, et les
  contrats successifs sont chaînés ;
- les affectations conservent le département pour l'historique et disposent
  d'une date de fin facultative.

La migration `V17__create_onboarding_block.sql` crée le modèle de checklist,
les onboardings, les tâches, les documents et l'inscription à la paie.

### Propositions d'embauche

Le backend expose la création d'une proposition pour une candidature
`RETENU`, sa réponse par le candidat et les versions de négociation :

- `POST /api/candidatures/{id}/propositions` (RH/ADMIN)
- `POST /api/propositions/{id}/repondre` (lien candidat public)
- `POST /api/propositions/{id}/ajuster` (RH/ADMIN)
- `POST /api/propositions/{id}/rejeter` (RH/ADMIN)
- `GET /api/candidatures/{id}/propositions` (RH/ADMIN ou manager propriétaire)

Le budget de la demande est bloquant, la validité d'un lien est de 15 jours
et trois versions maximum sont autorisées. Le jeton candidat est transmis
une seule fois dans la réponse de création/ajustement, seul son hash est
stocké en base, et il est invalidé dès la réponse.

À l'acceptation, le service crée dans la même transaction le dossier
`employe` en `EN_ONBOARDING`, un contrat `BROUILLON` et une affectation sur
le poste de la demande. Le dossier candidat existant est réutilisé si le
candidat était déjà salarié ; un nouveau contrat et une nouvelle affectation
sont alors ajoutés sans créer de second employé. La candidature passe à
`EMBAUCHE`, l'offre est clôturée lorsque le quota est atteint et
`EmployeCreeEvent` est publié pour le Bloc 4.

Les décisions de conception retenues sont les suivantes :

- la candidature est publique et ne nécessite pas de compte candidat ;
- `candidature.cv_snapshot` conserve une copie JSONB figée du CV au dépôt ;
- les tests et entretiens utilisent le modèle unique `evaluation`, distingué
  par `type_evaluation` ;
- une offre est liée à une demande et peut couvrir plusieurs postes ;
- une offre suit `BROUILLON -> PUBLIEE -> CLOTUREE` ;
- une candidature suit le cycle `RECU`, présélection, test, entretien, puis
  `RETENU` ou `REJETE` ;
- `candidature_historique` est append-only pour conserver chaque changement
  de statut.

### Contrats successifs

Les contrats validés peuvent être prolongés par la RH sans chevauchement :

- `POST /api/contrats/{id}/renouveler` crée un contrat du même type,
  commençant le lendemain de la fin du précédent ;
- `POST /api/contrats/{id}/convertir-cdi` crée un CDI à partir d'un CDD,
  également le lendemain de la fin du CDD ;
- la règle limite le renouvellement d'un CDD à une seule fois. Un CDI ne
  peut pas être renouvelé ;
- le nouveau contrat est créé en `BROUILLON`, puis suit le circuit de
  signature et validation. À sa validation, le contrat précédent passe à
  `TERMINE` ;
- `GET /api/employes/{id}/contrats` renvoie toute la chaîne dans l'ordre des
  dates de début ;
- `GET /api/contrats/echeances?jours=30` renvoie les contrats `VALIDE` dont
  la date de fin se trouve dans la période demandée. Une tâche planifiée
  quotidienne signale ces échéances dans les logs.

## Onboarding et checklist

L'événement `EmployeCreeEvent` déclenche la création idempotente d'un
onboarding et de ses tâches à partir du modèle standard. La date de début est
celle de l'affectation courante, avec repli sur la date d'embauche.

Endpoints disponibles :

- `GET /api/onboardings` et `GET /api/onboardings/{id}` ;
- `GET /api/employes/{id}/onboarding` ;
- `POST /api/onboarding-taches/{id}/demarrer`,
  `/terminer` et `/non-applicable` ;
- `POST /api/onboardings/{id}/taches` pour une tâche `AUTRE` ;
- `PATCH /api/onboarding-taches/{id}/responsable` pour réassigner ;
- `POST /api/onboardings/{id}/cloturer`.

Les transitions de tâche exigent un commentaire. La progression est calculée
par le serveur à partir du nombre de tâches `FAIT`. La clôture exige que
toutes les tâches obligatoires soient `FAIT`, active l'employé et publie
`OnboardingTermineEvent`.

## Bloc 4 : onboarding et suivi post-embauche — base de données

La migration `V17__create_onboarding_block.sql` applique les décisions du
bloc 4 :

- le bloc 3 crée le premier contrat ; les contrats suivants utilisent
  `contrat_precedent_id`, `numero_renouvellement` et le nouveau `motif`
  (`RENOUVELLEMENT`, `PASSAGE_CDI` ou `AVENANT`) ;
- le type de contrat courant est déduit du contrat actif, sans colonne
  redondante dans `employe` ;
- un employé ne peut avoir qu'un onboarding, avec les états
  `A_FAIRE`, `EN_COURS`, `TERMINE` et `ANNULE` ;
- la checklist standard est définie par `onboarding_modele` et
  `onboarding_modele_tache`. Les responsables sont portés par un rôle
  (`RH`, `IT`, `SERVICES_GENERAUX`, `MANAGER`) et peuvent être affectés à
  une personne via `assignee_id` ;
- les tâches suivent `A_FAIRE -> EN_COURS -> FAIT` ou
  `NON_APPLICABLE`. Les tâches obligatoires incluent le contrat, le compte,
  le badge, le matériel, la formation et la paie ;
- une inscription à la paie `INSCRIT` exige une date, une référence, un mode
  de paiement et un numéro de compte ;
- les documents sont conservés avec leur employé, leur tâche éventuelle,
  leur URL et leur taille. Les contraintes et index nécessaires sont créés
  par Flyway.

Les onboardings existants reçoivent automatiquement la checklist standard.
Le statut de démonstration est réparti entre les quatre états lorsque
plusieurs employés existent ; aucun faux employé n'est créé par la migration.

## Bloc 5 : dossier salarié et gouvernance des données

La migration `V21__create_employee_dossier.sql` fait de `employe` le point
d'entrée du dossier salarié. L'identité d'origine reste liée à `candidat` ;
les informations administratives complémentaires sont stockées dans
`employe`, qui devient la source de vérité après l'embauche.

Les adresses, contacts et salaires sont historisés par périodes
`date_debut`/`date_fin`. PostgreSQL empêche les chevauchements et garantit un
seul contact principal et un seul salaire courant. Les documents réutilisent
la table existante `document`, avec un type référentiel, un statut
`DEPOSE`, `VALIDE`, `REJETE` ou `EXPIRE`, une confidentialité, une version et
les dates de validité.

Le journal `journal_audit` est append-only : un trigger refuse toute
modification ou suppression. Les rôles et habilitations applicatives seront
appliqués par les services du dossier ; les champs CIN, CNaPS, situation
familiale, coordonnées bancaires et documents sont classés sensibles ou très
sensibles dans le référentiel.

### Situation professionnelle et carrière

Les endpoints suivants couvrent la situation courante et les évolutions :

- `GET /api/employes/{id}/carriere` fusionne contrats, affectations et salaires
  dans l'ordre chronologique ;
- `POST /api/employes/{id}/affectations` clôt l'affectation courante et crée la
  nouvelle affectation avec contrôle des cycles de management ;
- `POST /api/employes/{id}/salaires` conserve l'historique des rémunérations ;
- `POST /api/employes/{id}/sortie` enregistre une démission, un licenciement,
  une fin de contrat ou une retraite et archive ultérieurement le dossier.

L'ancienneté est calculée depuis la première date de contrat disponible dans la
chaîne contractuelle. Les événements `AffectationChangeeEvent`,
`SalaireChangeEvent` et `EmployeSortiEvent` sont publiés pour les blocs
suivants.
