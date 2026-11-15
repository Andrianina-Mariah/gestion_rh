<template>
  <section class="page">
    <div class="page-heading"><div><h2>Fiche d'onboarding</h2><p v-if="onboarding" class="muted">{{ onboarding.employeNom }} — {{ onboarding.statut }}</p></div><router-link class="button" to="/onboardings">Retour</router-link></div>
    <div v-if="error" class="alert error">{{ error }}</div>
    <template v-if="onboarding">
      <section class="card"><strong>Progression : {{ onboarding.progression }}%</strong> <progress :value="onboarding.progression" max="100"></progress><div class="muted">Début : {{ onboarding.dateDebut }} · Fin prévue : {{ onboarding.dateFinPrevue }}</div><p>Cette checklist organise les préparatifs de l'arrivée. Ce ne sont pas des validations hiérarchiques : chaque service réalise ses étapes, puis la RH vérifie l'ensemble et clôture l'onboarding.</p></section>
      <section v-for="group in groups" :key="group.name" class="card"><h3>{{ group.name }}</h3>
        <div v-for="task in group.tasks" :key="task.id" class="evaluation-row">
          <div class="page-heading"><strong>{{ task.libelle }}</strong><span>{{ task.statut }} · échéance {{ task.dateEcheance }}</span></div>
          <div class="muted">{{ task.assigneeNom || task.responsableRole }} · {{ task.obligatoire ? 'Obligatoire' : 'Facultative' }}</div>
          <div v-if="!canAct(task)" class="muted">Responsable : {{ task.assigneeNom || roleLabel(task.responsableRole) }}. La RH peut traiter toutes les tâches ; les autres comptes ne traitent que celles qui leur sont attribuées.</div>
          <div v-else-if="['FAIT', 'NON_APPLICABLE'].includes(task.statut)" class="muted">Étape traitée<span v-if="task.commentaire"> : {{ task.commentaire }}</span></div>
          <div v-else class="actions">
            <button v-if="task.statut === 'A_FAIRE'" class="button" @click="transition(task, 'demarrer')" title="Marquer le début de cette étape">Démarrer</button>
            <button class="button primary" @click="transition(task, 'terminer')" title="Confirmer que cette étape est terminée">Terminer</button>
            <button class="button" @click="transition(task, 'non-applicable')" title="Écarter cette étape avec une justification">Non applicable</button>
            <button v-if="task.type === 'BADGE'" class="button" @click="badge(task)">Enregistrer le badge remis</button>
            <button v-if="task.type === 'MATERIEL'" class="button" @click="material(task)">Enregistrer le matériel remis</button>
            <button v-if="task.type === 'FORMATION'" class="button" @click="training(task)">Enregistrer la formation</button>
            <button v-if="isRh" class="button" @click="reassign(task)">Réassigner</button>
          </div>
        </div>
      </section>
      <section v-if="isRh" class="card form-grid">
        <h3 class="full">Inscription à la paie</h3>
        <label>Référence paie <input v-model="payroll.referencePaie"></label>
        <label>Mode de paiement <select v-model="payroll.modePaiement"><option value="VIREMENT">Virement</option><option value="CHEQUE">Chèque</option></select></label>
        <label>Numéro de compte <input v-model="payroll.numeroCompte"></label>
        <div class="actions full"><button class="button primary" @click="registerPayroll">Inscrire à la paie</button></div>
      </section>
      <section v-if="isRh" class="card form-grid">
        <h3 class="full">Ajouter une tâche</h3>
        <label>Libellé <input v-model="newTask.libelle"></label><label>Responsable <input v-model="newTask.responsableRole"></label>
        <label>Échéance <input v-model="newTask.dateEcheance" type="date"></label><label>Obligatoire <input v-model="newTask.obligatoire" type="checkbox"></label>
        <div class="actions full"><button class="button primary" @click="addTask">Ajouter</button><button class="button primary" @click="closeOnboarding">Clôturer</button></div>
      </section>
    </template>
  </section>
</template>
<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import api from '@/api/axios'
import { formatApiError } from '@/utils/errors'
import { useAuthStore } from '@/stores/auth'
const route = useRoute(); const auth = useAuthStore(); const onboarding = ref(null); const error = ref('')
const isRh = computed(() => auth.hasRole(['RH', 'ADMIN']))
function roleLabel(role) { return ({ RH: 'Ressources humaines', IT: 'Informatique', SERVICES_GENERAUX: 'Services généraux', MANAGER: 'Manager' })[role] || role }
function canAct(task) { return isRh.value || (task.assigneeId === auth.user?.id) || (!task.assigneeId && task.responsableRole === auth.role) }
const newTask = ref({ libelle: '', responsableRole: 'RH', dateEcheance: '', obligatoire: true })
const payroll = ref({ referencePaie: '', modePaiement: 'VIREMENT', numeroCompte: '' })
const groups = computed(() => Object.entries((onboarding.value?.taches || []).reduce((acc, task) => { (acc[task.responsableRole] ||= []).push(task); return acc }, {})).map(([name, tasks]) => ({ name, tasks })))
async function load() { try { onboarding.value = (await api.get(`/onboardings/${route.params.id}`)).data } catch (err) { error.value = formatApiError(err) } }
async function transition(task, action) { const commentaire = window.prompt(action === 'non-applicable' ? 'Pourquoi cette tâche ne s’applique-t-elle pas ?' : 'Commentaire sur cette étape :'); if (commentaire === null || !commentaire.trim()) return; try { await api.post(`/onboarding-taches/${task.id}/${action}`, { commentaire: commentaire.trim() }); await load() } catch (err) { error.value = formatApiError(err) } }
async function addTask() { try { await api.post(`/onboardings/${route.params.id}/taches`, newTask.value); newTask.value = { libelle: '', responsableRole: 'RH', dateEcheance: '', obligatoire: true }; await load() } catch (err) { error.value = formatApiError(err) } }
async function closeOnboarding() { if (!window.confirm('Clôturer cet onboarding ?')) return; try { await api.post(`/onboardings/${route.params.id}/cloturer`); await load() } catch (err) { error.value = formatApiError(err) } }
async function badge(task) { const numero = window.prompt('Numéro du badge'); if (!numero) return; try { await api.post(`/onboarding-taches/${task.id}/badge`, { numero, dateRemise: new Date().toISOString().slice(0, 10), commentaire: 'Badge remis' }); await load() } catch (err) { error.value = formatApiError(err) } }
async function material(task) { const serial = window.prompt('Numéro de série du matériel'); if (!serial) return; try { await api.post(`/onboarding-taches/${task.id}/materiels`, { typeMateriel: 'Équipement', numeroSerie: serial, commentaire: 'Matériel remis' }); await load() } catch (err) { error.value = formatApiError(err) } }
async function training(task) { const trainer = window.prompt('Nom du formateur'); if (!trainer) return; try { await api.post(`/onboarding-taches/${task.id}/formation`, { dateFormation: new Date().toISOString().slice(0, 10), dureeHeures: 1, formateur: trainer, commentaire: 'Formation planifiée' }); await load() } catch (err) { error.value = formatApiError(err) } }
async function reassign(task) { const assigneeId = window.prompt('Identifiant du responsable'); if (!assigneeId) return; try { await api.patch(`/onboarding-taches/${task.id}/responsable`, { assigneeId: Number(assigneeId), responsableRole: task.responsableRole }); await load() } catch (err) { error.value = formatApiError(err) } }
async function registerPayroll() { try { await api.post(`/onboardings/${route.params.id}/paie`, payroll.value); await load() } catch (err) { error.value = formatApiError(err) } }
onMounted(load)
</script>
