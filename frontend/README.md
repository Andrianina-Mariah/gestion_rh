# Frontend Gestion RH (Vue 3 + Vite)

## Installation

```bash
npm install
npm run dev
```

## Build

```bash
npm run build
```

## Fonctionnalités implémentées

- Authentification (page de connexion + store Pinia `auth`)
- Garde de navigation (routes protégées et contrôle par rôle)
- Menu dynamique selon le rôle connecté
- Référentiels:
  - Départements: liste + création/modification/suppression
  - Postes: liste + filtre département + création/modification/suppression
- Demandes manager:
  - Mes demandes (pagination + filtre statut)
  - Formulaire (brouillon, soumission, suppression) avec validations client
  - Détail en lecture seule (modification uniquement si brouillon)
- Consultation RH/Finance/Direction:
  - Liste globale avec filtres statut/département/période
  - Détail en lecture seule
- Traitement du workflow:
  - Liste "À traiter" adaptée au rôle
  - Actions RH, Finance et Direction avec confirmation
  - Historique chronologique et indicateur de progression
  - Correction et resoumission Manager
  - Ouverture du recrutement par RH
- Notifications:
  - Cloche avec compteur de notifications non lues
  - Liste avec liens vers les demandes
  - Rafraîchissement automatique toutes les 45 secondes
- Tableau de bord:
  - Compteurs par statut pour RH et Direction
  - Dix dernières demandes

## Bloc 2 : recrutement

L'espace public est disponible sans authentification :

- `/offres` : liste des offres publiées ;
- `/offres/:id` : détail d'une offre et candidature publique en quatre étapes
  (identité, profil, expériences, formations et pièces jointes).

L'espace interne `/gestion-offres` est accessible à la RH, au Manager et à
l'ADMIN :

- gestion des brouillons, publication et clôture ;
- partage du lien public et impression ;
- candidatures filtrées par statut et pagination ;
- détail du dossier, décisions de présélection, évaluations et historique ;
- liste des candidats retenus.

Le formulaire public envoie le CV et la lettre au backend en multipart. Les
fichiers respectent les limites et types contrôlés par l'API.

## Bloc 4 : onboarding

L'espace onboarding est accessible aux RH, ADMIN, managers et responsables IT :

- `/onboardings` : liste filtrable avec progression ;
- `/onboardings/:id` : checklist groupée par responsable, transitions,
  réassignation, tâches badge/matériel/formation et inscription à la paie ;
- `/mes-taches-integration` : tâches assignées triées par échéance ;
- `/contrats/echeances` : contrats à échéance avec renouvellement ou conversion CDI.

## Comptes (seed backend)

- `manager@sirh.mg`
- `rh@sirh.mg`
- `finance@sirh.mg`
- `direction@sirh.mg`
- `admin@sirh.mg`

cd /home/maria/s5/Tovo/gestion-rh/frontend

npm install        # seulement la première fois
npm run build
npm run dev

cd /home/maria/s5/Tovo/gestion-rh/backend

mvn -DskipTests compile
mvn spring-boot:run
