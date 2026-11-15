# TODO - SIRH - Bloc 7 : Congés et absences

Stack : Java 21 (Spring Boot) / Vue.js 3 / PostgreSQL

Prérequis : blocs 5 et 6 terminés (dossier employé, manager hiérarchique, planning, jours fériés, variables de paie).

Périmètre : types de congés, droits, soldes, demande -> contrôle du solde -> validation manager puis RH, absences et justificatifs, contrôle des chevauchements, mise à jour du planning et des variables de paie.
Hors périmètre : calcul de la paie.

Légende : [ ] à faire, [x] terminé

---

## 0. Décisions de conception

- [ ] Objets : `TypeConge`, `DroitConge`, `SoldeConge`, `DemandeConge`, `Absence`
- [ ] Différence entre congé et absence : le congé est demandé à l'avance et validé ; l'absence est constatée (maladie, absence injustifiée, événement familial, mission) et peut être justifiée après coup
- [ ] Types de congés à gérer : annuel payé, maladie, maternité, paternité, événements familiaux, sans solde, récupération. Lesquels débitent un solde, lesquels non ? (à confirmer avec le Code du travail malgache et la politique interne)
- [ ] Acquisition des droits : règle de calcul (ex. 2,5 jours par mois de service effectif selon le Code du travail malgache, à confirmer), prorata à l'entrée, ancienneté, report d'une année sur l'autre et plafond de report
- [ ] Unité : jours ouvrables ou ouvrés ? demi-journées autorisées ? Les jours fériés et repos dans la période ne sont pas décomptés
- [ ] Circuit de validation : manager puis RH ; que se passe-t-il si le manager est absent (délégation) ou s'il n'y a pas de manager ?
- [ ] Machine à états de la demande :

```
BROUILLON -> SOUMISE
SOUMISE -> VALIDEE_MANAGER | REFUSEE
VALIDEE_MANAGER -> VALIDEE_RH | REFUSEE
VALIDEE_RH -> ANNULEE (avant ou pendant le congé, avec règles)
SOUMISE | VALIDEE_MANAGER -> ANNULEE (par le demandeur)
```

- [ ] Réservation du solde : à la soumission (solde "en attente") ou à la validation finale ? Proposition : réserver à la soumission pour éviter les doubles demandes
- [ ] Préavis minimum, durée maximale consécutive, périodes de blocage (ex. clôture), seuil de personnes en congé simultanément dans un service : règles à décider
- [ ] Justificatifs : obligatoires pour la maladie au-delà de N jours ; délai pour les fournir
- [ ] Un employé en onboarding ou en période d'essai a-t-il droit au congé ? Règle à décider

## 1. Base de données

- [ ] `type_conge` : `code` (UNIQUE), `libelle`, `remunere`, `debite_solde`, `justificatif_obligatoire`, `justificatif_apres_jours`, `duree_max_jours`, `demi_journee_autorisee`, `actif`
- [ ] `droit_conge` : `type_conge_id`, `regle_acquisition` (jours par mois ou par an), `anciennete_min_mois`, `plafond_report`, `validite_report_mois` ; peut dépendre du contrat ou de l'ancienneté
- [ ] `solde_conge` : `employe_id`, `type_conge_id`, `annee`, `acquis`, `pris`, `en_attente`, `reporte`, `ajuste` ; `UNIQUE (employe_id, type_conge_id, annee)` ; `CHECK (pris >= 0 AND en_attente >= 0)`
- [ ] `mouvement_solde` : journal des mouvements (ACQUISITION, PRISE, RESERVATION, LIBERATION, REPORT, AJUSTEMENT, EXPIRATION) avec `solde_conge_id`, `quantite`, `demande_id`, `date`, `motif`, `cree_par_id`. Le solde se vérifie par la somme des mouvements
- [ ] `demande_conge` : `employe_id`, `type_conge_id`, `date_debut`, `date_fin`, `demi_journee_debut`, `demi_journee_fin`, `nb_jours` (calculé et stocké), `motif`, `statut` (CHECK), `valide_manager_id`, `date_validation_manager`, `valide_rh_id`, `date_validation_rh`, `motif_refus`, `date_soumission`
- [ ] `CHECK (date_fin >= date_debut)` sur la demande
- [ ] `absence` : `employe_id`, `type` (MALADIE, INJUSTIFIEE, EVENEMENT_FAMILIAL, MISSION, AUTRE), `date_debut`, `date_fin`, `statut` (CONSTATEE, JUSTIFIEE, NON_JUSTIFIEE), `demande_conge_id` (nullable), `source` (POINTAGE, SAISIE_RH, DEMANDE), `commentaire`
- [ ] `justificatif` : `absence_id` ou `demande_conge_id`, lien vers `document` (bloc 5), `statut` (DEPOSE, VALIDE, REJETE), `date_depot`
- [ ] `delegation_validation` : `manager_id`, `delegue_id`, `date_debut`, `date_fin`
- [ ] Anti-chevauchement : contrainte d'exclusion PostgreSQL (`EXCLUDE USING gist` sur `employe_id` et `daterange`) pour les demandes actives (hors REFUSEE et ANNULEE)
- [ ] Index : `demande_conge(employe_id, statut)`, `demande_conge(date_debut, date_fin)`, `solde_conge(employe_id, annee)`, `absence(employe_id, date_debut)`
- [ ] Migrations Flyway et jeu de test (une demande par statut, soldes variés, chevauchements)

## 2. Backend : types, droits et soldes

- [ ] Entités et DTO : `TypeConge`, `DroitConge`, `SoldeConge`, `MouvementSolde`
- [ ] CRUD des types de congés et des règles de droits (RH / ADMIN)
- [ ] Calcul de l'acquisition mensuelle : tâche planifiée qui crédite les soldes, prorata à l'entrée, idempotente (pas de double crédit pour un même mois)
- [ ] Report annuel : tâche de fin d'année avec plafond et expiration des jours non reportables
- [ ] Ajustement manuel du solde par la RH, avec motif obligatoire et audit
- [ ] `GET /api/employes/{id}/soldes` et `GET /api/moi/soldes` (acquis, pris, en attente, restant)
- [ ] Solde disponible = acquis + reporté + ajusté - pris - en attente ; jamais négatif sauf type explicitement autorisé
- [ ] Initialisation des soldes pour un nouvel employé (à l'activation) et recalcul si le contrat change

## 3. Backend : demande de congé

- [ ] `POST /api/demandes-conge` (création en BROUILLON) puis `POST /api/demandes-conge/{id}/soumettre`
- [ ] Calcul de `nb_jours` : exclut week-ends, jours de repos du planning et jours fériés (bloc 6), gère les demi-journées
- [ ] Contrôle du solde à la soumission : refus si solde insuffisant ; réservation (`en_attente`) dans la même transaction
- [ ] Contrôle des chevauchements : avec une autre demande active du même employé et avec une absence déjà enregistrée
- [ ] Autres règles : préavis minimum, durée maximale, périodes de blocage, quota de présence minimale du service
- [ ] Validation manager : `POST /api/demandes-conge/{id}/valider-manager`, `/refuser` (motif obligatoire) ; seul le manager du demandeur ou son délégué peut agir
- [ ] Validation RH : `POST /api/demandes-conge/{id}/valider-rh` ; débit définitif du solde (réservation -> prise)
- [ ] Refus ou annulation : libération de la réservation dans la même transaction
- [ ] Annulation d'un congé validé : `POST /api/demandes-conge/{id}/annuler` avec règles (avant le début ou en cours, recrédit proportionnel) et validation RH
- [ ] Idempotence : une validation répétée ne débite pas deux fois
- [ ] Verrouillage optimiste ou pessimiste sur le solde pour éviter deux demandes simultanées qui dépassent le solde
- [ ] Consultation : `GET /api/demandes-conge` avec filtres (statut, période, département), vue "à valider" pour le manager et la RH
- [ ] Événements : `CongeValideEvent`, `CongeAnnuleEvent` (consommés par le planning et la paie)

## 4. Backend : absences et justificatifs

- [ ] Saisie d'une absence par la RH ou le manager : `POST /api/absences`
- [ ] Création automatique des absences à qualifier depuis les anomalies de pointage du bloc 6 (journée sans pointage ni congé)
- [ ] Qualification : transformer une absence en maladie, en congé (avec débit du solde), en absence injustifiée
- [ ] Dépôt d'un justificatif (certificat médical, etc.) avec contrôle du type et de la taille, via `document` ; validation ou rejet par la RH
- [ ] Délai de justification : absence passée en NON_JUSTIFIEE si aucun justificatif n'arrive dans le délai
- [ ] Rappel au salarié des justificatifs manquants
- [ ] Une absence ne peut chevaucher un congé validé : contrôle ou fusion
- [ ] `GET /api/employes/{id}/absences` et rapport d'absentéisme (taux, durée moyenne, par service)

## 5. Backend : planning et variables de paie

- [ ] À la validation RH : mise à jour du planning (jours en type CONGE) pour la période, et retour à TRAVAIL à l'annulation
- [ ] Les absences mettent à jour le planning (type ABSENCE) et neutralisent les anomalies de pointage correspondantes
- [ ] Production des variables de paie : jours de congé payé, congé sans solde, absence injustifiée, maladie (selon `remunere`), transmises à la période du bloc 6
- [ ] Gestion d'un congé à cheval sur deux périodes de paie (répartition des jours par mois)
- [ ] Interdiction de modifier un congé ou une absence situés sur une période de temps déjà verrouillée (bloc 6), sauf réouverture
- [ ] Notifications : demande soumise (manager), décision (demandeur), rappel de validation en attente, justificatif manquant, solde faible
- [ ] Calendrier d'équipe des congés : `GET /api/equipes/{id}/calendrier-conges?du=&au=`

## 6. Frontend

### 6.1 Espace salarié

- [ ] Mes soldes par type (acquis, pris, en attente, restant)
- [ ] Formulaire de demande : type, dates, demi-journées, calcul du nombre de jours et du solde restant en direct, avertissement en cas de chevauchement ou de solde insuffisant
- [ ] Mes demandes avec statut, historique, annulation
- [ ] Dépôt de justificatif
- [ ] Mes absences

### 6.2 Espace manager

- [ ] Demandes à valider avec le contexte : solde, calendrier de l'équipe, personnes déjà absentes
- [ ] Validation / refus avec motif
- [ ] Calendrier de l'équipe, délégation de validation

### 6.3 Espace RH

- [ ] Configuration des types de congés et des règles de droits
- [ ] File de validation RH, ajustement manuel de soldes
- [ ] Saisie et qualification des absences, validation des justificatifs
- [ ] Rapports : soldes par service, absentéisme, congés à venir, jours non pris à risque d'expiration
- [ ] Calendrier global

## 7. Tests

- [ ] Unitaires : calcul de `nb_jours` (week-ends, fériés, demi-journées), acquisition mensuelle et prorata, report et expiration
- [ ] Unitaires : transitions de la demande, règles de préavis et de durée maximale
- [ ] Intégration : soumission -> réservation -> validation manager -> validation RH -> débit et mise à jour du planning, en transaction
- [ ] Intégration : refus ou annulation qui libère le solde
- [ ] Chevauchements : même employé, congé et absence, bornes exactes (fin le jour du début suivant)
- [ ] Concurrence : deux demandes simultanées sur un solde insuffisant, une seule passe
- [ ] Idempotence : double validation et double crédit mensuel sans effet
- [ ] Sécurité : un manager ne valide que son équipe, un employé ne voit que ses demandes, délégation respectée
- [ ] Bout en bout : demande -> validation -> planning mis à jour -> pointage non anormal -> variable de paie produite

## 8. Documentation

- [ ] Tableau des types de congés avec règles de solde, justificatifs et rémunération
- [ ] Règles d'acquisition, de report et de calcul des jours, avec exemples chiffrés
- [ ] Diagrammes d'états : demande de congé, absence
- [ ] Règles de gestion du bloc 7 (suite de la numérotation)
- [ ] Schéma de base et OpenAPI à jour

## 9. Critères de fin

- [ ] Le circuit demande -> contrôle du solde -> validation manager -> validation RH fonctionne
- [ ] Les soldes sont justes, traçables par mouvements, et jamais débités deux fois
- [ ] Aucun chevauchement de congés ou d'absences n'est possible pour un employé
- [ ] Les absences sont justifiées ou qualifiées, avec gestion des justificatifs
- [ ] Le planning et les variables de paie sont mis à jour automatiquement
- [ ] Les tests passent, la documentation est à jour

---

## Ordre d'exécution recommandé

1. Sections 0 et 1 (décisions, migrations)
2. Section 2 (types, droits, soldes), puis 6.3 partie configuration
3. Section 3 (demande), puis 6.1 et 6.2
4. Sections 4 et 5 (absences, planning, paie), puis 6.3 partie absences et rapports
5. Sections 7, 8, 9
