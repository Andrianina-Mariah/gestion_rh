# Guide de tests manuels — SIRH, blocs 1 à 7

Ce guide transforme les parcours décrits dans `todo_bloc01.md` à `todo_bloc07.md` en scénarios de test partageables. Il privilégie les parcours de bout en bout, avec les connexions successives nécessaires et les résultats à vérifier.

> Les TODO décrivent le périmètre attendu, mais toutes les cases ne sont pas cochées dans ces fichiers. Ce guide porte sur les fonctions visibles dans l'application et les API actuelles. Si une action indiquée n'existe pas dans l'écran, ne la considérez pas comme réussie : consignez-la comme écart et vérifiez l'endpoint via Swagger (`http://localhost:8080/swagger-ui/index.html`) s'il est disponible.

## 1. Préparer l'environnement et les comptes

### Démarrage

Dans deux terminaux, depuis la racine du dépôt :

```bash
cd backend
docker compose up -d postgres
mvn spring-boot:run
```

```bash
cd frontend
npm install   # première installation seulement
npm run dev
```

Ouvrir l'application Vite, généralement `http://localhost:5173`. Le backend écoute sur `http://localhost:8080`. Attendre que Flyway ait terminé les migrations avant les tests.

Le démarrage du backend initialise les mots de passe des comptes existants avec la valeur de développement `password`.

### Comptes de test disponibles

| Connexion             | Mot de passe | Usage principal                                                                  |
| --------------------- | ------------ | -------------------------------------------------------------------------------- |
| `manager@sirh.mg`   | `password` | Demandes de recrutement, décisions d'évaluation et parcours manager            |
| `rh@sirh.mg`        | `password` | Référentiels, validations RH, recrutement, contrats, dossier, temps et congés |
| `finance@sirh.mg`   | `password` | Analyse budgétaire des demandes du bloc 1                                       |
| `direction@sirh.mg` | `password` | Validation finale des demandes du bloc 1                                         |
| `admin@sirh.mg`     | `password` | Administration et vérification des droits privilégiés                         |
| `it@sirh.mg` | `password` | Préparation du compte et du matériel dans l'onboarding |
| `services.generaux@sirh.mg` | `password` | Remise du badge dans l'onboarding |

Le candidat au recrutement n'a pas de compte. Il postule depuis l'espace public. Lorsqu'un contrat est validé, un compte salarié est créé à l'adresse email du candidat avec le mot de passe temporaire `ChangeMe123!`.

### Règles pour les testeurs

- Suivez l'ordre des blocs ci-dessous sur une base de test, de préférence vide pour le premier parcours complet. Le bloc 2 dépend d'une demande de recrutement ouverte au bloc 1.
- Gardez les identifiants créés pendant le parcours : ID demande, ID offre, email candidat, matricule, ID employé, période de paie.
- Après chaque action, notez le statut affiché et reconnectez-vous avec le rôle suivant pour vérifier la visibilité et l'action attendues.
- N'effectuez pas de test de suppression ou de réinitialisation sur une base partagée. Les écritures de test (demande, candidature, salarié, pointage) sont persistantes.
- Les dates de congé et les pointages historiques doivent respecter les contraintes de préavis et de période ouvertes. Utilisez des dates futures pour les congés et des dates passées récentes pour tester le CSV de pointage.

## 2. Bloc 1 — Besoin en personnel et circuit de validation

### Scénario 1A — Référentiels département et poste

1. Connectez-vous comme **RH** (`rh@sirh.mg`). Ouvrez **Départements** et créez un département de test, par exemple `Equipe Test SIRH`.
2. Ouvrez **Postes**, créez un poste `Analyste QA` rattaché à ce département, puis revenez à la liste.
3. Modifiez la description du poste. Vérifiez que le département et la modification sont conservés.
4. Essayez de créer un poste sans titre ou sans département : l'application doit refuser l'enregistrement.
5. Connectez-vous comme **Manager** et vérifiez que la création/modification des référentiels n'est pas proposée et que les routes RH lui sont interdites.

**Résultat attendu :** le département et le poste apparaissent dans leurs listes RH ; le poste peut ensuite être sélectionné dans une demande.

### Scénario 1B — Demande, analyse budgétaire et ouverture du recrutement

1. Connectez-vous comme **Manager** (`manager@sirh.mg`), ouvrez **Mes demandes** puis **Nouvelle demande**.
2. Créez une demande en brouillon : poste `Analyste QA`, type `CREATION`, au moins un poste, motif explicite, budget positif, date souhaitée future. Enregistrez.
3. Rouvrez le brouillon, vérifiez ses valeurs, puis soumettez-le. Notez son ID.
4. Connectez-vous comme **RH**, ouvrez **À traiter** ou la notification reçue. Vérifiez le demandeur, le poste et le statut. Choisissez de demander l'analyse budgétaire et ajoutez un commentaire si demandé.
5. Connectez-vous comme **Finance**, ouvrez **À traiter**, retrouvez la demande, renseignez le budget validé et acceptez l'analyse.
6. Connectez-vous comme **Direction**, validez la demande en attente de décision finale.
7. Reconnectez-vous comme **RH**, ouvrez la demande devenue validée, puis lancez **Ouvrir le recrutement**.
8. Reconnectez-vous comme **Manager** et vérifiez le statut `RECRUTEMENT_OUVERT`, l'historique des étapes et les notifications.

**Variantes à vérifier :**

- Soumettre un budget nul/négatif ou une date passée : refus côté formulaire ou API.
- Depuis Finance, tentez de traiter une demande qui n'est pas en analyse : l'action doit être absente ou refusée.
- Dans une seconde demande, choisissez le chemin sans Finance : RH → Direction → ouverture RH.
- Dans une demande de test séparée, RH ou Finance demande une correction ; le Manager la corrige et la resoumet. Vérifiez que le cycle et l'historique précédent restent visibles.
- Rejetez une demande de test : son statut devient définitif et elle ne peut plus être ouverte en recrutement.

## 3. Bloc 2 — Publication d'offre et recrutement

### Scénario 2A — Offre et candidature publique

1. Après le scénario 1B, connectez-vous comme **RH**, ouvrez **Offres et candidatures**. Retrouvez le brouillon associé à la demande ouverte.
2. Complétez le titre, le profil, le lieu et le type de contrat, enregistrez puis publiez l'offre.
3. Utilisez **Partager** ou copiez le lien public. Ouvrez-le dans une fenêtre privée/sans session : l'offre doit être visible sans connexion.
4. Comme **candidat externe**, déposez une candidature avec une nouvelle adresse email de test, un profil, une expérience/formation si le formulaire le propose, un CV et une lettre autorisés. Notez l'adresse utilisée.
5. Déposez une seconde candidature avec la même adresse sur la même offre : elle doit être refusée comme doublon. Une candidature sur une autre offre publiée doit rester possible.
6. Connectez-vous comme **RH**, ouvrez les candidatures de l'offre et vérifiez que la nouvelle candidature apparaît en `RECU`, avec ses pièces et son historique.

### Scénario 2B — Présélection, liste d'attente, évaluations et décision finale

1. En **RH**, depuis le détail d'une candidature, acceptez un dossier (`ACCEPTER`) et donnez un commentaire. Vérifiez le passage en présélection.
2. Placez une autre candidature en liste d'attente, puis vérifiez son rang. Si nécessaire, promouvez-la après avoir accepté les premiers dossiers.
3. Pour la candidature présélectionnée, planifiez un `TEST_TECHNIQUE` à une date future et un évaluateur autorisé. Vérifiez que l'évaluation apparaît.
4. Connectez-vous comme **Manager** propriétaire de l'offre ou comme évaluateur. Saisissez une note, un résultat et une remarque. Vérifiez le changement d'étape.
5. Si le test est accepté, planifiez l'entretien requis, saisissez son résultat, puis retournez comme RH pour prendre la décision finale `RETENU`.
6. Vérifiez l'historique chronologique, le candidat retenu et la notification RH. Pour une candidature distincte, testez le refus avec commentaire obligatoire.
7. Testez le rejet d'une offre clôturée : une nouvelle candidature publique doit être impossible.

**Résultat attendu :** un candidat `RETENU` est visible dans **Candidats retenus** et peut entrer dans le bloc 3.

## 4. Bloc 3 — Proposition, embauche et contrat

### Scénario 3A — Proposition et négociation du candidat

1. En **RH**, ouvrez **Propositions d'embauche** et sélectionnez le candidat retenu du scénario 2B.
2. Envoyez une proposition avec date de début future, lieu, salaire et type de contrat `CDD` ou `CDI`. Le salaire doit respecter le budget de la demande.
3. Copiez le lien sécurisé affiché après création. Ouvrez-le dans une fenêtre privée, sans compte RH.
4. Comme candidat, choisissez **Négocier**, saisissez une remarque, puis envoyez la réponse.
5. Retournez comme RH : vérifiez la notification et l'historique. Ajustez la proposition pour créer une nouvelle version.
6. Ouvrez le lien sécurisé de la nouvelle version et acceptez. Notez que le lien est à usage limité et que le candidat n'a toujours pas eu à créer un compte.

**Variantes :** refus candidat ; tentative de réutilisation d'un jeton après réponse ; proposition dépassant le budget ; troisième version au-delà de la limite permise.

### Scénario 3B — Employé et contrat

1. Après acceptation, comme **RH**, ouvrez **Employés**. Vérifiez la création d'un seul employé lié au candidat, le matricule généré, le statut `EN_ONBOARDING`, l'affectation, et le contrat brouillon.
2. Ouvrez la fiche employé et vérifiez les informations de carrière et le contrat. Ne créez pas un deuxième dossier pour la même personne.
3. Terminez le contrat selon les actions proposées dans l'application : création, signature/dépôt si demandé, validation RH. Vérifiez le statut final `VALIDE`.
4. Connectez-vous avec l'email candidat du nouvel employé et le mot de passe temporaire `ChangeMe123!`. Vérifiez l'accès salarié après validation du contrat.
5. Testez un type de contrat à durée déterminée avec date de fin conforme. Les variantes renouvellement et passage en CDI se testent ensuite au bloc 4.

## 5. Bloc 4 — Contrats successifs et onboarding

### Scénario 4A — Checklist et inscription à la paie

Ce bloc est une checklist de préparation de l'arrivée du salarié, pas une suite de validations hiérarchiques. Chaque service exécute ses tâches ; la RH coordonne, peut dépanner les tâches depuis la fiche, inscrit le salarié à la paie et clôture quand les tâches obligatoires sont terminées.

Comptes de test disponibles après application de la migration V43 : `rh@sirh.mg`, `manager@sirh.mg`, `it@sirh.mg`, `services.generaux@sirh.mg` ; mot de passe initial `password`. Au premier démarrage, l'application chiffre ce mot de passe comme pour les autres comptes de démonstration.

1. Connectez-vous comme **RH**, ouvrez **Onboardings**, puis ouvrez le nouvel employé. Choisissez un onboarding dont les tâches sont encore à faire ; un onboarding déjà à 100 % ne permet pas de tester les actions.
2. Repérez le salarié affiché sur chaque tâche et son responsable. Contrat et compte peuvent être déjà `FAIT` automatiquement après validation du contrat ; c'est attendu.
3. Connectez-vous comme **IT** : dans **Mes tâches d'intégration**, la tâche Matériel doit nommer l'employé. Démarrez-la, puis enregistrez le matériel avec un numéro de série fictif.
4. Connectez-vous comme **Services généraux** : démarrez puis enregistrez la remise d'un badge fictif.
5. Connectez-vous comme **Manager** : démarrez puis enregistrez la formation d'intégration. Le Manager organise cette étape ; il ne valide pas le contrat ou la paie.
6. Revenez comme **RH**, ouvrez la fiche du même salarié et inscrivez-le à la paie avec des valeurs fictives. Les tâches RH peuvent également être traitées dans cette fiche.
7. Avant de terminer toutes les tâches obligatoires, tentez **Clôturer** : l'application doit refuser. Terminez les tâches restantes puis clôturez et vérifiez le statut employé `ACTIF`.

Dans les listes, `Démarrer` marque le début d'une tâche ; `Terminer` confirme sa réalisation avec un commentaire ; `Non applicable` l'écarte avec une justification. `Réassigner` est réservé à la RH et change le compte utilisateur responsable. Les tâches déjà faites n'ont plus de boutons d'action.

### Scénario 4B — Historique de contrats

1. Comme **RH**, ouvrez **Échéances contrats**. Vérifiez un contrat CDD/CE arrivant dans la fenêtre choisie.
2. Renouvelez un CDD valide une première fois, puis signez/validez le nouveau contrat. Vérifiez le chaînage et la clôture de l'ancien contrat.
3. Tentez un deuxième renouvellement CDD : il doit être refusé.
4. Sur un autre contrat à durée déterminée valide, testez la conversion en CDI. Vérifiez qu'il n'a pas de date de fin et que l'ancien contrat reste dans l'historique.
5. Consultez la fiche employé et sa frise de carrière : les deux contrats doivent être conservés et chronologiques.

## 6. Bloc 5 — Dossier administratif

### Scénario 5A — Dossier RH, documents et carrière

1. En **RH**, ouvrez **Employés**, recherchez le salarié créé, puis ouvrez sa fiche.
2. Vérifiez identité, coordonnées, situation professionnelle, contrat actif, affectation et carrière.
3. Déposez un document de test (par exemple CV ou diplôme) et contrôlez son statut. Validez-le comme RH ; sur un document distinct, rejetez-le avec un motif.
4. Essayez un fichier trop volumineux ou d'un type non autorisé : l'API doit le refuser.
5. Enregistrez une nouvelle affectation (mutation) ou un changement de salaire avec date et motif de test. Vérifiez que l'ancienne affectation/le salaire historique reste consultable.
6. Avec **Admin/Auditeur**, ouvrez le journal d'audit, filtrez sur l'employé et vérifiez les opérations RH pertinentes.

### Scénario 5B — Self-service et contrôle des accès

1. Connectez-vous avec le compte salarié `ChangeMe123!`. Ouvrez **Mon dossier**, vérifiez les informations et déposez un document.
2. Envoyez une demande de modification de téléphone/adresse si l'écran la propose. Vérifiez son apparition côté RH.
3. Connectez-vous comme RH et approuvez/refusez cette demande dans la file prévue ; vérifiez le résultat côté salarié.
4. Tentez d'ouvrir l'URL de la fiche d'un autre employé avec un rôle salarié : l'accès doit être refusé.
5. Connectez-vous comme Manager et vérifiez que la liste d'équipe n'expose pas le salaire, CIN ou coordonnées bancaires d'un autre salarié.

## 7. Bloc 6 — Temps de travail et pointage

### Scénario 6A — Régime, planning et jour férié

1. En **RH**, ouvrez **Gestion du temps**. Créez un régime de test (par exemple 40 h / 5 jours, tolérance de 10 minutes).
2. Sélectionnez le régime et définissez les horaires du lundi au vendredi, avec pause. Enregistrez.
3. Assignez-le au salarié de test pour une plage de dates sans chevaucher une autre assignation.
4. Générez le planning de l'employé sur une période de quelques semaines. Vérifiez les jours travaillés et repos.
5. Ajoutez un jour férié de test à une date de la plage, régénérez ou consultez le planning : le jour férié ne doit pas être généré comme jour travaillé.
6. Connectez-vous comme salarié et ouvrez **Mon temps** : planning et date doivent correspondre. Comme Manager, vérifiez **Temps de l'équipe** pour un salarié de son équipe.

### Scénario 6B — Pointage normal, retard, anomalie et correction

1. Comme salarié, ouvrez **Mon temps**. Cliquez **Pointer mon entrée**, vérifiez l'heure serveur et l'état « entrée enregistrée ». Cliquez ensuite **Pointer ma sortie**.
2. Essayez une double entrée ou double sortie. La séquence doit être refusée ou signalée comme anomalie ; un double clic rapproché doit être ignoré/refusé.
3. Pour tester une journée passée et un retard, utilisez l'import CSV RH. Colonnes exactes : `matricule,date_heure,sens`. Exemple (séparateur virgule ou point-virgule accepté) :

   ```csv
   matricule,date_heure,sens
   EMP-AAAA-0001,2026-11-04T08:18:00+03:00,ENTREE
   EMP-AAAA-0001,2026-11-04T17:00:00+03:00,SORTIE
   ```

   Remplacez le matricule et choisissez une date passée récente dont la période reste ouverte. Importez aussi une ligne invalide et un doublon pour contrôler le rapport de lignes rejetées.
4. Consultez le récapitulatif mensuel salarié. Vérifiez minutes travaillées, retard, départ anticipé et éventuelles heures supplémentaires selon l'horaire configuré.
5. Comme RH, ouvrez les anomalies. Traitez une anomalie avec commentaire. Comme Manager, validez/refusez une demande d'heures supplémentaires ou une correction de votre équipe.
6. Comme salarié, vérifiez que la justification d'anomalie est visible et que l'historique de pointage conserve la correction sans écraser l'origine.

### Scénario 6C — Clôture et export des variables

1. En RH, créez/ouvrez une période de test. Vérifiez les anomalies ouvertes de la période.
2. Tentez la validation tant qu'une anomalie ouverte existe : elle doit échouer (sauf dérogation tracée, si prévue).
3. Traitez les anomalies, puis validez la période. Vérifiez son verrouillage et la génération des variables (quantités, pas montants de salaire).
4. Exportez en JSON puis CSV. Vérifiez matricule, type et quantités.
5. Tentez de corriger un pointage de la période verrouillée : l'action doit échouer. Réouvrez avec un motif, vérifiez l'audit, corrigez/recalculez puis validez de nouveau.

## 8. Bloc 7 — Congés et absences

### Scénario 7A — Paramétrage RH et solde salarié

1. En **RH**, ouvrez **Congés et absences** → **Gestion RH**.
2. Créez un type `CONGE_TEST_PAYE` rémunéré, débiteur de solde, autorisant demi-journées, unité `OUVRES`.
3. Ajoutez une règle de droit pour ce type (par exemple acquisition mensuelle de test), puis vérifiez qu'elle apparaît dans le tableau.
4. Pour une demande immédiate, sélectionnez un employé et faites un ajustement positif de quelques jours avec un motif obligatoire. Notez que l'ajustement doit être traçable.
5. Ouvrez **Soldes par service** et vérifiez que les jours restants/reportés sont agrégés au bon service.

### Scénario 7B — Demande du salarié, manager, RH

1. Connectez-vous comme salarié avec le compte créé au bloc 3. Ouvrez **Congés et absences** → **Mon espace**. Vérifiez les soldes (acquis, pris, en attente, restant).
2. Choisissez un type qui débite le solde, une date future suffisamment éloignée pour respecter le préavis, puis une plage incluant un jour ouvré. Vérifiez l'estimation et le solde restant affichés.
3. Soumettez la demande. Le statut doit devenir `SOUMISE` et les jours demandés passent en attente/réservation.
4. Connectez-vous comme **Manager** affecté à l'employé. Ouvrez les validations, affichez le contexte (solde, congés validés et absences), puis validez. Vérifiez le statut `VALIDEE_MANAGER`.
5. Connectez-vous comme **RH**, ouvrez la file RH et validez. Vérifiez `VALIDEE_RH`, le solde pris et le planning passé à `CONGE`.
6. Retournez en salarié : vérifiez statut, dates, historique et absence de débit double. Consultez aussi le calendrier d'équipe.

**Variantes à tester sur d'autres demandes :**

- solde insuffisant à la soumission ; chevauchement avec une demande active ; chevauchement avec une absence ; dates en sens inverse ; demi-journée ; week-end/jour férié ; préavis trop court ; période bloquée ; quota minimal de présence du service.
- refus Manager avec motif obligatoire : réservation libérée et statut `REFUSEE`.
- annulation salarié avant validation RH : réservation libérée.
- annulation RH d'un congé validé : solde recrédité selon les règles et planning restauré.
- délégation : comme Manager, créez une délégation datée vers un membre de votre équipe. Connectez-vous comme délégué et validez une demande sur la période de délégation ; vérifiez qu'un autre manager ne peut pas agir.

### Scénario 7C — Absence et justificatif

1. Comme **RH** ou **Manager** affecté, saisissez une absence de test sur une date sans congé validé.
2. Vérifiez que le planning passe à `ABSENCE` et que l'anomalie de pointage correspondante n'est plus ouverte.
3. Connectez-vous comme salarié et déposez un justificatif PDF/PNG/JPEG valide depuis **Mes absences**. Essayez aussi un type interdit.
4. Comme RH, ouvrez les justificatifs déposés, validez-en un et rejetez-en un autre avec motif. Vérifiez que l'état de l'absence suit la décision.
5. Qualifiez une absence de test en maladie, absence injustifiée ou congé avec débit. Vérifiez le solde et la variable de paie produite selon le type/rémunération.
6. Vérifiez le rapport d'absentéisme et le calendrier global.

### Scénario 7D — Variables de paie, période verrouillée et répartition mensuelle

1. Utilisez une période du bloc 6 encore `OUVERTE`. Validez-la après traitement des anomalies.
2. Dans les variables/export, vérifiez les quantités de jours de congé payé, congé sans solde, maladie rémunérée/non rémunérée et absence injustifiée. Il ne doit pas y avoir de calcul de montant de paie.
3. Faites un congé de test qui chevauche deux mois, validez-le puis vérifiez que les jours sont répartis dans les deux périodes mensuelles concernées.
4. Tentez de modifier/annuler une absence ou un congé sur une période verrouillée : l'action doit être refusée. Après réouverture RH avec motif, retestez l'opération et le recalcul.

## 9. Contrôles transversaux à faire avec deux utilisateurs

| Test                  | Premier utilisateur               | Vérification avec le second                                                          |
| --------------------- | --------------------------------- | ------------------------------------------------------------------------------------- |
| Notification          | Manager soumet une demande        | RH reçoit la notification ; le Manager voit ensuite les décisions                   |
| Accès par rôle      | RH crée/configure un objet       | Manager/Finance/Direction ne voient que les actions de leur rôle                     |
| Données d'équipe    | Salarié pointe ou pose un congé | Manager affecté voit seulement son équipe et peut traiter ses éléments            |
| Confidentialité      | RH consulte ou modifie le dossier | Salarié voit son dossier ; autre salarié n'accède pas au dossier du premier        |
| Historique            | Manager/RH valide ou corrige      | Le demandeur voit statut et historique ; l'ancien pointage/document reste traçable   |
| Période verrouillée | RH valide/exporte une période    | Toute modification temps/congé/absence est refusée jusqu'à réouverture autorisée |

## 10. Fiche de résultat à remplir par l'équipe

Copier ce bloc pour chaque scénario exécuté :

```text
Bloc / scénario :
Testeur :
Date et environnement (URL / branche / base) :
Utilisateur(s) utilisé(s) :
Préconditions et IDs (demande, offre, employé, période...) :
Étapes exécutées :
Résultat attendu :
Résultat observé :
Statut : PASS / FAIL / BLOQUÉ / NON TESTÉ
Capture ou message d'erreur :
Ticket / commentaire :
```

## 11. Préconditions qui bloquent souvent les tests

- Aucun employé n'est créé par le seul fait d'avoir un candidat retenu : il faut accepter une proposition et valider le contrat.
- Sans salarié avec email candidat et mot de passe connu, les parcours **Mon dossier**, **Mon temps** et **Mon espace congé** ne peuvent pas être testés en rôle `EMPLOYE`.
- Pour tester les validations manager, l'affectation du salarié doit référencer un manager. Une affectation sans `managerId` ne constitue pas une équipe exploitable.
- Les migrations V8+ alimentent des candidats/propositions de démonstration sous certaines conditions (offre issue d'une demande ouverte). Si la base a déjà été migrée avant la création de cette demande, les migrations ne rejouent pas les insertions : faites le parcours de bout en bout plutôt que de compter sur ces exemples.
- Le calcul des congés dans le formulaire est une estimation ; le serveur recalcule selon régime, planning et jours fériés.
- Le bloc 6 et le bloc 7 ne calculent pas la paie : ils exportent des durées/quantités.
