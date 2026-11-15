# TODO - SIRH - Bloc 4 : Contrat et onboarding (intégration)

Stack : Java 21 (Spring Boot) / Vue.js 3 / PostgreSQL

Prérequis : bloc 3 terminé (employé créé en statut EN_ONBOARDING, contrat, affectation, événement `EmployeCreeEvent`).

Périmètre : contrats successifs (renouvellement, passage CDD -> CDI) -> checklist d'onboarding -> workflow d'intégration -> inscription à la paie (état de paie) -> clôture de l'onboarding.
Hors périmètre : dossier administratif complet (bloc 5), calcul de la paie.

Légende : [ ] à faire, [x] terminé

---

## 0. Décisions de conception

- [ ] Frontière avec le bloc 3 : le bloc 3 crée le premier contrat ; le bloc 4 gère les contrats suivants (renouvellement, CDD -> CDI) et l'onboarding
- [ ] Ne jamais stocker seulement `employe.type_contrat` : le type courant est déduit du contrat actif (historique conservé)
- [ ] Onboarding : un onboarding par employé, généré automatiquement à la réception de `EmployeCreeEvent`
- [ ] Tâches standard de la checklist : contrat signé, badge, compte, matériel, formation, inscription paie. Les tâches sont définies par un modèle (`OnboardingModele`) pour pouvoir varier selon le poste ou le département
- [ ] Machine à états de l'onboarding : `A_FAIRE -> EN_COURS -> TERMINE` (ou `ANNULE` si l'embauche tombe)
- [ ] Machine à états d'une tâche : `A_FAIRE -> EN_COURS -> FAIT` (ou `NON_APPLICABLE`)
- [ ] Qui est responsable de chaque tâche : RH (contrat, paie), IT (compte, matériel), services généraux (badge), manager (formation). Rôle ou personne nominative ?
- [ ] Le compte utilisateur est créé au bloc 3 (validation du contrat) : la tâche "compte" de la checklist est donc cochée automatiquement
- [ ] Inscription à la paie ("Etat de paie : mandray karama") : tâche bloquante pour clôturer l'onboarding ; elle exige contrat VALIDE, salaire, coordonnées de paiement
- [ ] Que faire si l'employé ne se présente pas (date de début dépassée) : annulation de l'onboarding, règle à décider

## 1. Base de données

- [ ] `onboarding` : `id`, `employe_id` (UNIQUE), `statut` (CHECK), `date_debut`, `date_fin_prevue`, `date_cloture`, `responsable_id`
- [ ] `onboarding_modele` et `onboarding_modele_tache` : liste des tâches par défaut (libellé, rôle responsable, délai en jours par rapport à la date de début, obligatoire oui/non)
- [ ] `onboarding_tache` : `onboarding_id`, `libelle`, `type` (CONTRAT, BADGE, COMPTE, MATERIEL, FORMATION, PAIE, AUTRE), `statut` (CHECK), `responsable_role`, `assignee_id`, `date_echeance`, `date_realisation`, `obligatoire`, `commentaire`
- [ ] `document` : `id`, `employe_id`, `onboarding_tache_id` (nullable), `type`, `nom_fichier`, `url`, `taille`, `date_depot`, `depose_par_id`
- [ ] `contrat` : vérifier `numero_renouvellement` et `contrat_precedent_id` (créés au bloc 3) et ajouter `motif` (RENOUVELLEMENT, PASSAGE_CDI, AVENANT)
- [ ] Inscription paie : table `inscription_paie` (`employe_id`, `date_inscription`, `statut`, `reference_paie`, `mode_paiement`, `numero_compte`)
- [ ] Contraintes : une seule tâche d'un type donné par onboarding (sauf AUTRE), `date_echeance >= date_debut`
- [ ] Index : `onboarding_tache(onboarding_id, statut)`, `onboarding_tache(assignee_id, statut)`, `document(employe_id)`
- [ ] Migrations Flyway et jeu de test (un onboarding par statut)

## 2. Backend : contrats successifs

- [ ] Renouvellement : `POST /api/contrats/{id}/renouveler` crée un nouveau contrat chaîné (`contrat_precedent_id`, `numero_renouvellement + 1`)
- [ ] Passage CDD -> CDI : `POST /api/contrats/{id}/convertir-cdi` (nouveau contrat sans date de fin)
- [ ] Règles de durée : CE 6 mois renouvelable, CDD 2 ans renouvelable une seule fois, CDI sans fin ; refus de tout renouvellement non conforme (à confirmer par la recherche du bloc 3)
- [ ] Pas de chevauchement : le nouveau contrat commence au lendemain de la fin du précédent
- [ ] Le contrat précédent passe à l'état TERMINE (ou RENOUVELE) à la validation du nouveau
- [ ] Alerte de fin de contrat : liste des contrats arrivant à échéance dans N jours (`GET /api/contrats/echeances?jours=30`) et tâche planifiée quotidienne
- [ ] Historique : `GET /api/employes/{id}/contrats` retourne la chaîne complète dans l'ordre

## 3. Backend : onboarding et checklist

- [ ] Entités `Onboarding`, `OnboardingTache`, `OnboardingModele` et DTO
- [ ] Écouteur de `EmployeCreeEvent` : crée l'onboarding et ses tâches à partir du modèle (idempotent : un seul onboarding par employé)
- [ ] Calcul des dates d'échéance à partir de la date de début de l'affectation
- [ ] Endpoints : `GET /api/onboardings`, `GET /api/onboardings/{id}`, `GET /api/employes/{id}/onboarding`
- [ ] Transitions de tâche : `POST /api/onboarding-taches/{id}/demarrer`, `/terminer`, `/non-applicable` (avec commentaire obligatoire)
- [ ] Ajout manuel d'une tâche personnalisée (`POST /api/onboardings/{id}/taches`) tant que l'onboarding n'est pas clôturé
- [ ] Réassigner une tâche à un autre responsable
- [ ] Progression : pourcentage de tâches faites, calculé côté serveur
- [ ] Clôture : `POST /api/onboardings/{id}/cloturer` possible seulement si toutes les tâches obligatoires sont FAIT ; l'employé passe de EN_ONBOARDING à ACTIF
- [ ] Événement `OnboardingTermineEvent` (pour le bloc 5)

## 4. Backend : tâches spécifiques

- [ ] Contrat signé : tâche cochée automatiquement quand le contrat passe à VALIDE
- [ ] Compte : cochée automatiquement à la création du compte utilisateur
- [ ] Badge : saisie du numéro de badge et de la date de remise ; unicité du numéro
- [ ] Matériel : liste du matériel attribué (ordinateur, téléphone, ...) avec numéro de série ; table `materiel_attribue`
- [ ] Formation : inscription à une ou plusieurs formations d'accueil (date, durée, formateur)
- [ ] Inscription paie : création de `inscription_paie` après contrôle des prérequis (contrat VALIDE, salaire, mode de paiement) ; cochée automatiquement
- [ ] Documents : dépôt et téléchargement de fichiers rattachés à une tâche, avec contrôle du type et de la taille
- [ ] Notifications : onboarding créé, tâche assignée, échéance proche ou dépassée, onboarding terminé

## 5. Frontend

### 5.1 Espace RH

- [ ] Liste des onboardings (filtres : statut, département, date de début) avec barre de progression
- [ ] Fiche d'onboarding : checklist groupée par responsable, actions Démarrer / Terminer / Non applicable
- [ ] Ajout et réassignation de tâches
- [ ] Page des contrats arrivant à échéance, avec actions Renouveler / Convertir en CDI
- [ ] Frise chronologique des contrats successifs d'un employé
- [ ] Formulaire d'inscription à la paie

### 5.2 Espace responsable de tâche (IT, services généraux, manager)

- [ ] "Mes tâches d'intégration" : tâches assignées, triées par échéance
- [ ] Formulaires dédiés : badge, matériel, formation

### 5.3 Espace nouvel employé (facultatif)

- [ ] Vue en lecture seule de sa propre checklist et de ses documents à fournir

## 6. Tests

- [ ] Unitaires : transitions de tâche et d'onboarding, règles de renouvellement, calcul des échéances
- [ ] Intégration : `EmployeCreeEvent` crée un onboarding complet, sans doublon si l'événement est reçu deux fois
- [ ] Clôture refusée tant qu'une tâche obligatoire n'est pas faite
- [ ] Renouvellement : CDD renouvelé une fois accepté, une deuxième fois refusé ; chaînage correct
- [ ] Bout en bout : employé créé -> checklist -> toutes les tâches -> inscription paie -> onboarding clôturé -> employé ACTIF
- [ ] Sécurité : accès par rôle (un responsable ne voit que ses tâches)

## 7. Documentation

- [ ] Diagrammes d'états : onboarding et tâche
- [ ] Règles de gestion du bloc 4 (suite de la numérotation)
- [ ] Modèle de checklist par défaut documenté
- [ ] Schéma de base et OpenAPI à jour

## 8. Critères de fin

- [ ] Un salarié peut avoir plusieurs contrats successifs, l'historique n'est jamais écrasé
- [ ] La création d'un employé génère automatiquement sa checklist d'onboarding
- [ ] Les tâches contrat, badge, compte, matériel, formation et paie sont suivies
- [ ] L'inscription à la paie est enregistrée avant la clôture
- [ ] Les contrats proches de l'échéance sont signalés
- [ ] Les tests passent, la documentation est à jour

---

## Ordre d'exécution recommandé

1. Sections 0 et 1 (décisions, migrations)
2. Section 2 (contrats successifs)
3. Sections 3 et 4 (onboarding, tâches), puis 5.1 et 5.2
4. Section 5.3 si le temps le permet
5. Sections 6, 7, 8
