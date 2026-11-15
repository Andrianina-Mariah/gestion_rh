# Conception d'un logiciel de gestion RH

## Cycle personnel :

Besoin
Recrutement
Embauche
Vie du salarie
Evolution
Rupture

## Objectif pedagogiques

Comprendre les processus avant de concevoir les fonctionnalites du logiciel.
Transformer un processus metier en regles de gestion, donnees, workflows et interfaces.
Concevoir un SIRH (Systeme d'information des ressources humaines)...

### **Bloc 1 - Expression du besoin en personnel**

- Un manager exprime un besoin : creation ou remplacement d'un poste.
- Informations : poste, departement, effectif, motif, budget, date souhaite.
- Workflow : Manager -> RH -> Direction -> ouverture recrutements.
- Objets metier : Departement, poste, DemandeRecrutement, Validation.
- Point de conception : un POSTE existe independamment de l'EMPLOYE.

### **Bloc 2 - Recrutement**

- Transformer une demande validee en offre d'emploi.
- Publier l'offre et recevoir plusieurs candidatures.
- Preselectionner, organiser les entretiens et les tests.
- Conserver les evaluations et la decision finale.
- Objets: Offres d'emploi, Candidat,  Candidature , Entetien, EvaluationCandidat

**`Le plus important dans une application : tracabilite (oviana dia aiza?)`**

### **Bloc 3 - Selection et embauche**

- Le candidat retenu une proposition d'embauche.
- Apres acceptation : creation du dossier salarie et du matricule.
- Creer le contrat, le poste et l'affectation.
- Point de conception : CANDIDAT != EMPLOYE.
- Une personne peut avoir plusieurs candidatures avant de devenir salariee.
  **NB: acceptation = tous ce qui est dans contrat de travail**
  apres acceptation -> tokony CE (contrat d'essai)

### **Bloc 4 - Contrat et onboarding(au bord / tafiditra)**

- Un salarie peut avoir plusieurs contrats successifs : CDD, renouvellement, CDI(Durre indeterminer), ...
- Ne pas ecraser l'historique en stockant seulement employe.type_contrat.
- Onboarding : contrat signe, badge, compte, materiel, formation, inscription paie.
- Utiliser une checklist et un workflow d'integration.
- Objets : Contrat, Affectation, Onboarding, OnboardingTache, Document.

**Etat de paie : misoratra ao @ mandray karama**

# -> Application web

reporting, notification, mobile tsy voatery

Ny andrasana: manifestation des besoins -> onboarding

### **Bloc 5 - Dossier administratif du salarié**

* Centraliser identité, coordonnées, documents et sistuation professionnelle.
* Conserver contrats, affectations, salaire et historique de carrière.
* Gérer les documents administratifs et leur validité.
* Prévoir confidentialité, habitations et journal d'audit.
* Le dossier salarié devient le référentiel central du SIRH

### **Bloc 6 - Temps de travail et pointage**

* Définir horaires, planning et régime de travail.
* Enregistrer entrées, sorties, retards et heures supplémentaires.
* Calculer le temps réellement travaillé.
* Détecter les anomalies de pointage.
* Les données de temps alimentent les absences et les variables de paie.

### **Bloc 7 - Congés et absences**

* Demande de congé -> contrôle du solde -> validation manager/RH.
* Gérer droits, soldes, types de congés, absences et justificatifs.
* Contrôler chevauchements et disponibilité du solde.
* Mettre à jour planning et variables de paie.
* Objets : TypeConge, DroitConge, SoldeConge, DemandeConge, Absence.
