<template>
  <section class="page">
    <div class="page-heading"><div><h2>Mes tâches d'intégration</h2><p class="muted">Tâches qui vous sont attribuées, triées par échéance.</p></div></div>
    <div v-if="error" class="alert error">{{ error }}</div>
    <div class="alert info">Une tâche décrit une étape pratique de l'arrivée du salarié, ce n'est pas une validation de demande. « Démarrer » marque le début, « Terminer » confirme que l'étape est faite, « Non applicable » l'écarte avec une justification. La RH coordonne et peut traiter toutes les tâches depuis la fiche d'onboarding.</div>
    <div class="card"><div v-for="task in tasks" :key="task.id" class="evaluation-row">
      <div class="page-heading"><strong>{{ task.libelle }}</strong><span>Pour : {{ task.employeNom }} ({{ task.matricule }}) · échéance {{ task.dateEcheance }}</span></div>
      <div>{{ roleLabel(task.responsableRole) }} · {{ task.statut }}<span v-if="task.commentaire"> · {{ task.commentaire }}</span></div>
      <router-link class="link-button" :to="{ name: 'onboarding-detail', params: { id: task.onboardingId } }">Ouvrir la fiche d'onboarding</router-link>
      <div v-if="['FAIT', 'NON_APPLICABLE'].includes(task.statut)" class="muted">Cette tâche est déjà traitée ; aucune action n'est nécessaire.</div>
      <div v-else class="actions"><button v-if="task.statut === 'A_FAIRE'" class="button" @click="change(task, 'demarrer')">Démarrer</button><button class="button primary" @click="change(task, 'terminer')">Terminer</button><button class="button" @click="change(task, 'non-applicable')">Non applicable</button></div>
    </div><div v-if="!tasks.length" class="empty">Aucune tâche assignée.</div></div>
  </section>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import api from '@/api/axios'
import { formatApiError } from '@/utils/errors'
import { useAuthStore } from '@/stores/auth'
const auth = useAuthStore()
const tasks = ref([]); const error = ref('')
function roleLabel(role) { return ({ RH: 'Ressources humaines', IT: 'Informatique', SERVICES_GENERAUX: 'Services généraux', MANAGER: 'Manager' })[role] || role }
async function load() {
  try {
    const onboardings = (await api.get('/onboardings')).data
    tasks.value = onboardings.flatMap(o => (o.taches || [])
      .filter(t => t.assigneeId === auth.user?.id || (!t.assigneeId && t.responsableRole === auth.role))
      .map(t => ({ ...t, onboardingId: o.id, employeNom: o.employeNom, matricule: o.matricule })))
      .sort((a, b) => a.dateEcheance.localeCompare(b.dateEcheance))
  } catch (err) { error.value = formatApiError(err) }
}
async function change(task, action) { const commentaire = window.prompt(action === 'non-applicable' ? 'Pourquoi cette tâche ne s’applique-t-elle pas ?' : 'Commentaire sur cette étape :'); if (commentaire === null || !commentaire.trim()) return; try { await api.post(`/onboarding-taches/${task.id}/${action}`, { commentaire: commentaire.trim() }); await load() } catch (err) { error.value = formatApiError(err) } }
onMounted(load)
</script>
