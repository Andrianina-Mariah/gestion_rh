# TODO - SIRH - Bloc 6 : Temps de travail et pointage

Stack : Java 21 (Spring Boot) / Vue.js 3 / PostgreSQL

Prérequis : bloc 5 terminé (employés ACTIFS, affectations, manager hiérarchique).

Périmètre : horaires, régimes de travail, planning, pointage (entrées, sorties), retards, heures supplémentaires, temps réellement travaillé, anomalies, variables de paie.
Hors périmètre : calcul de la paie, gestion des congés (bloc 7, mais le bloc 6 expose les points d'intégration).

Légende : [ ] à faire, [x] terminé

---

## 0. Décisions de conception

- [ ] Règle centrale : les données de temps alimentent les absences (bloc 7) et les variables de paie ; le bloc 6 ne calcule pas de salaire, il produit des quantités (heures normales, heures supplémentaires, retards, absences)
- [ ] Régimes de travail à supporter : temps plein (40 h par semaine selon le Code du travail malgache, à confirmer), temps partiel, horaires par équipes, horaires flexibles. Lesquels pour la première version ?
- [ ] Horaire théorique d'un employé : défini par un régime assigné à l'employé avec dates de validité (historique), pas par un champ unique
- [ ] Moyen de pointage : saisie web, bouton entrée / sortie, import d'un fichier de pointeuse, ou les trois ? Source du pointage conservée (`WEB`, `IMPORT`, `MANUEL`)
- [ ] Pointage manuel / correction : autorisé uniquement avec motif et validation du manager ; la donnée d'origine n'est jamais écrasée (correction = nouvelle ligne ou historique)
- [ ] Règles de retard : tolérance en minutes (ex. 10), seuil de déclenchement, retard cumulé
- [ ] Règles d'heures supplémentaires : déclenchement après l'horaire théorique, validation préalable ou a posteriori par le manager, plafond hebdomadaire et annuel, taux de majoration (30 %, 50 %, nuit, dimanche, jours fériés : à confirmer par le Code du travail)
- [ ] Jours fériés et jours chômés : calendrier configurable par année
- [ ] Fuseau horaire : stockage en UTC, affichage en heure locale (Madagascar, UTC+3)
- [ ] Machine à états de la journée de pointage : `OUVERTE -> CLOTUREE -> VALIDEE` (ou `EN_ANOMALIE`)
- [ ] Verrouillage de période : une période validée / exportée vers la paie n'est plus modifiable sans réouverture tracée

## 1. Base de données

- [ ] `regime_travail` : `code`, `libelle`, `heures_hebdo`, `jours_travailles_par_semaine`, `tolerance_retard_min`
- [ ] `horaire_type` : `regime_travail_id`, `jour_semaine` (1 à 7), `heure_debut`, `heure_fin`, `pause_minutes`, `travaille` (booleen)
- [ ] `employe_regime` : `employe_id`, `regime_travail_id`, `date_debut`, `date_fin` (historique, pas de chevauchement)
- [ ] `jour_ferie` : `date` (UNIQUE), `libelle`, `paye` (booleen)
- [ ] `planning` : `employe_id`, `date`, `heure_debut`, `heure_fin`, `type` (TRAVAIL, REPOS, CONGE, ABSENCE, FERIE), `source` (REGIME, MANUEL, CONGE) ; `UNIQUE (employe_id, date, heure_debut)`
- [ ] `pointage` : `employe_id`, `date_heure`, `sens` (ENTREE, SORTIE), `source`, `cree_par_id`, `motif_correction`, `pointage_origine_id`
- [ ] `journee_travail` : `employe_id`, `date`, `heure_arrivee`, `heure_depart`, `minutes_travaillees`, `minutes_retard`, `minutes_depart_anticipe`, `minutes_sup`, `statut` (CHECK), `UNIQUE (employe_id, date)`
- [ ] `heure_supplementaire` : `employe_id`, `date`, `minutes`, `taux`, `statut` (DEMANDEE, VALIDEE, REFUSEE), `valide_par_id`, `motif`
- [ ] `anomalie_pointage` : `employe_id`, `date`, `type` (ENTREE_MANQUANTE, SORTIE_MANQUANTE, DOUBLON, HORS_PLANNING, DUREE_EXCESSIVE, RETARD_NON_JUSTIFIE), `statut` (OUVERTE, JUSTIFIEE, CORRIGEE, IGNOREE), `commentaire`
- [ ] `periode_temps` : `annee`, `mois`, `statut` (OUVERTE, VALIDEE, EXPORTEE), pour le verrouillage
- [ ] `variable_paie` : `employe_id`, `periode_id`, `type` (HEURES_NORMALES, HEURES_SUP_30, HEURES_SUP_50, NUIT, DIMANCHE_FERIE, RETARD, ABSENCE), `quantite_minutes`, `source`
- [ ] Contraintes : `heure_fin > heure_debut` (ou gestion du passage minuit), `CHECK` sur les énumérations, pas deux régimes actifs en même temps
- [ ] Index : `pointage(employe_id, date_heure)`, `journee_travail(employe_id, date)`, `anomalie_pointage(statut, date)`, `variable_paie(periode_id, employe_id)`
- [ ] Migrations Flyway et jeu de test (régimes, jours fériés, journées normales et avec anomalies)

## 2. Backend : horaires, régimes et planning

- [ ] Entités et DTO : régime, horaire type, assignation employé-régime, jour férié
- [ ] CRUD des régimes et horaires types (RH), avec contrôle de cohérence des heures hebdomadaires
- [ ] Assignation d'un régime à un employé avec dates de validité, sans chevauchement
- [ ] Génération du planning : `POST /api/plannings/generer` pour un employé, un département ou une période, à partir du régime et en excluant les jours fériés
- [ ] Modification ponctuelle du planning (échange de jour, horaire exceptionnel) avec motif
- [ ] `GET /api/employes/{id}/planning?du=&au=` et vue d'équipe pour le manager
- [ ] Calendrier des jours fériés par année : `GET` et `POST /api/jours-feries`
- [ ] Hooks pour le bloc 7 : une absence ou un congé validé met à jour le planning (type CONGE / ABSENCE)

## 3. Backend : pointage

- [ ] `POST /api/pointages` (entrée ou sortie pour l'utilisateur connecté), l'employé est celui du jeton, jamais passé en paramètre
- [ ] Contrôle de séquence : une sortie suppose une entrée ouverte, pas deux entrées de suite (sinon anomalie)
- [ ] Horodatage fixé par le serveur, jamais par le client
- [ ] Import d'un fichier de pointeuse (CSV) avec rapport de lignes rejetées et protection contre les doublons
- [ ] Pointage manuel / correction par le manager ou la RH : motif obligatoire, ancien pointage conservé, journal d'audit
- [ ] `GET /api/employes/{id}/pointages?du=&au=` et `GET /api/pointages/aujourdhui` (qui est présent)
- [ ] Gestion du travail de nuit et des journées à cheval sur minuit
- [ ] Contrôle d'unicité par fenêtre de temps (double clic) : ignorer ou refuser deux pointages à quelques secondes d'écart

## 4. Backend : calcul du temps travaillé

- [ ] Service de calcul de la journée : à partir des pointages et du planning, produit `journee_travail` (minutes travaillées, retard, départ anticipé)
- [ ] Déduction de la pause (fixe ou pointée)
- [ ] Retard : arrivée après l'heure prévue + tolérance
- [ ] Heures supplémentaires : minutes au-delà du planning, rattachées à une demande validée ; sans validation, elles restent "en attente"
- [ ] Répartition par taux : heures normales, majorées, nuit, dimanche, jour férié (règles configurables)
- [ ] Plafonds : alerte au dépassement du quota d'heures supplémentaires ou de la durée maximale quotidienne
- [ ] Tâche planifiée de clôture quotidienne (recalcule la veille) et recalcul à la demande après correction
- [ ] Récapitulatif mensuel : `GET /api/employes/{id}/temps/recap?mois=`
- [ ] Heures supplémentaires : `POST /api/heures-sup` (demande), `/valider`, `/refuser` avec le manager

## 5. Backend : anomalies et variables de paie

- [ ] Détection automatique à la clôture de journée : entrée ou sortie manquante, doublon, pointage hors planning, durée excessive, absence sans congé
- [ ] `GET /api/anomalies` avec filtres (statut, type, période, département) ; traitement : justifier, corriger, ignorer (avec commentaire)
- [ ] Notification au salarié et au manager lors d'une anomalie ; rappel si non traitée après N jours
- [ ] Une période ne peut pas être validée tant qu'il reste des anomalies ouvertes (ou avec dérogation tracée)
- [ ] Validation de la période par la RH : `POST /api/periodes/{id}/valider`, verrouillage
- [ ] Production des `variable_paie` à la validation (heures normales, supplémentaires, retards, absences)
- [ ] Export pour la paie : `GET /api/periodes/{id}/variables-paie` (JSON et CSV), statut EXPORTEE
- [ ] Réouverture d'une période : droit restreint, motif, audit, recalcul des variables
- [ ] Événement `PeriodeTempsValideeEvent`
- [ ] Alimentation du bloc 7 : minutes d'absence non justifiées transmises comme absences à qualifier

## 6. Frontend

### 6.1 Espace salarié

- [ ] Bouton Pointer entrée / sortie avec heure et état courant
- [ ] Mon planning (semaine / mois) et mon historique de pointage
- [ ] Mes retards, mes heures supplémentaires, mon récapitulatif mensuel
- [ ] Justification d'une anomalie, demande d'heures supplémentaires

### 6.2 Espace manager

- [ ] Planning d'équipe et présence du jour
- [ ] Validation des heures supplémentaires et des corrections
- [ ] Liste des anomalies de l'équipe

### 6.3 Espace RH

- [ ] Gestion des régimes, horaires types, jours fériés
- [ ] Assignation des régimes, génération du planning
- [ ] Import des pointages, rapport d'import
- [ ] Tableau de bord des anomalies, validation et clôture de période
- [ ] Export des variables de paie
- [ ] Rapports : taux de retard, heures supplémentaires par département, absentéisme

## 7. Tests

- [ ] Unitaires : calcul du temps travaillé, retard avec tolérance, répartition des heures supplémentaires par taux, journée à cheval sur minuit
- [ ] Unitaires : détection de chaque type d'anomalie
- [ ] Intégration : pointage -> clôture de journée -> `journee_travail` correcte
- [ ] Intégration : import CSV avec doublons et lignes invalides
- [ ] Verrouillage : modification impossible sur une période validée, réouverture tracée
- [ ] Concurrence : deux pointages simultanés du même employé
- [ ] Sécurité : un employé ne pointe et ne lit que pour lui-même ; un manager ne valide que son équipe
- [ ] Bout en bout : planning généré -> pointages sur un mois -> anomalies traitées -> période validée -> variables de paie exportées

## 8. Documentation

- [ ] Règles de calcul (tolérance, pause, majorations, plafonds) avec exemples chiffrés
- [ ] Diagrammes d'états : journée de pointage, heure supplémentaire, anomalie, période
- [ ] Format du fichier d'import et de l'export paie
- [ ] Règles de gestion du bloc 6 (suite de la numérotation)
- [ ] Schéma de base et OpenAPI à jour

## 9. Critères de fin

- [ ] Horaires, régimes et planning sont définis et historisés
- [ ] Entrées, sorties, retards et heures supplémentaires sont enregistrés
- [ ] Le temps réellement travaillé est calculé automatiquement chaque jour
- [ ] Les anomalies de pointage sont détectées et traitées avant la validation de la période
- [ ] Les variables de paie et les absences à qualifier sont produites à partir des données de temps
- [ ] Les tests passent, la documentation est à jour

---

## Ordre d'exécution recommandé

1. Sections 0 et 1 (décisions, migrations)
2. Section 2 (régimes, planning), puis 6.3 partie configuration
3. Sections 3 et 4 (pointage, calcul), puis 6.1
4. Section 5 (anomalies, période, variables de paie), puis 6.2 et 6.3
5. Sections 7, 8, 9
