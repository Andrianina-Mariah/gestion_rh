<template>
  <section class="page">
    <div class="page-heading"><div><h2>Dossier de {{ candidature?.prenom }} {{ candidature?.nom }}</h2><p class="muted">CV figé, pièces jointes, évaluations et historique.</p></div>
      <router-link class="button" :to="{ name: 'offre-candidatures', params: { id: candidature?.offreId } }">Retour aux candidatures</router-link>
    </div>
    <div v-if="error" class="alert error">{{ error }}</div>
    <template v-if="candidature">
      <div class="card detail-grid">
        <div><strong>Email :</strong> {{ candidature.email }}</div><div><strong>Téléphone :</strong> {{ candidature.telephone || '—' }}</div>
        <div><strong>Profil :</strong> {{ candidature.titreProfil }}</div><div><strong>Statut :</strong> {{ candidature.statut }}</div>
        <div class="full"><strong>Résumé :</strong><p class="preformatted">{{ candidature.resume }}</p></div>
        <div class="full"><strong>Compétences :</strong><p class="preformatted">{{ candidature.competences }}</p></div>
        <div><button v-if="candidature.cvUrl" type="button" class="button" @click="openFile(candidature.cvUrl)">Voir le CV figé</button></div>
        <div><button v-if="candidature.lettreMotivationUrl" type="button" class="button" @click="openFile(candidature.lettreMotivationUrl)">Voir la lettre</button></div>
      </div>
      <section class="card" v-if="canAct">
        <h3>Décision du dossier</h3><label>Commentaire <textarea v-model="commentaire" rows="3" /></label>
        <div class="actions">
          <button class="button primary" @click="decide('ACCEPTER')">Accepter</button>
          <button class="button" @click="decide('LISTE_ATTENTE')">Liste d'attente</button>
          <button class="danger-button" @click="decide('REJETER')">Rejeter</button>
          <button v-if="candidature.statut === 'EN_ATTENTE'" class="button" @click="promote">Promouvoir</button>
        </div>
      </section>
      <section class="card" v-if="canEvaluate">
        <h3>Planifier une évaluation</h3>
        <div class="form-grid">
          <label>Type <select v-model="evaluation.type"><option>TEST_TECHNIQUE</option><option>ENTRETIEN_RH</option><option>ENTRETIEN_MANAGER</option></select></label>
          <label>Date <input v-model="evaluation.date" type="datetime-local" /></label>
          <label>Évaluateur ID <input v-model.number="evaluation.evaluateurId" type="number" min="1" /></label>
        </div>
        <button class="button primary" @click="schedule">Planifier</button>
      </section>
      <section class="card" v-if="evaluations.length">
        <h3>Évaluations planifiées</h3>
        <div v-for="item in evaluations" :key="item.id" class="evaluation-row">
          <strong>{{ item.type }}</strong> — {{ formatDate(item.date) }} — {{ item.resultat || 'À saisir' }}
          <form v-if="!item.resultat && canEvaluate" class="inline-form" @submit.prevent="result(item)">
            <input v-model="item.resultDate" type="datetime-local" required />
            <input v-model.number="item.note" type="number" min="0" max="20" step="0.01" placeholder="Note" />
            <input v-model="item.remarque" placeholder="Remarque" />
            <select v-model="item.resultatInput"><option>ACCEPTE</option><option>LISTE_ATTENTE</option><option>REJETE</option></select>
            <button class="button" type="submit">Enregistrer</button>
          </form>
        </div>
      </section>
      <section class="card" v-if="['ENTRETIEN_PASSE', 'TEST_PASSE'].includes(candidature.statut) && auth.role !== 'MANAGER'">
        <h3>Décision finale</h3><label>Commentaire <textarea v-model="finalComment" rows="2" /></label>
        <div class="actions"><button class="button primary" @click="finalDecision('RETENU')">Retenir</button><button class="danger-button" @click="finalDecision('REJETE')">Rejeter</button></div>
      </section>
      <section class="timeline card">
        <div class="section-heading"><h3>Historique des décisions</h3></div>
        <ol v-if="candidature.historique?.length">
          <li v-for="(item, index) in candidature.historique" :key="`${item.date}-${index}`">
            <div class="timeline-marker"></div>
            <div>
              <strong>{{ item.ancienStatut || 'Création' }} → {{ item.nouveauStatut }}</strong>
              <div class="muted">{{ item.utilisateur || 'Système' }} · {{ formatDate(item.date) }}</div>
              <p v-if="item.commentaire">{{ item.commentaire }}</p>
            </div>
          </li>
        </ol>
        <p v-else class="muted">Aucune décision enregistrée.</p>
      </section>
    </template>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import api from '@/api/axios'
import { useAuthStore } from '@/stores/auth'
import { formatApiError } from '@/utils/errors'

const route = useRoute(); const auth = useAuthStore()
const candidature = ref(null); const evaluations = ref([]); const error = ref('')
const commentaire = ref(''); const finalComment = ref('')
const evaluation = reactive({ type: 'TEST_TECHNIQUE', date: '', evaluateurId: null })
const canEvaluate = computed(() => ['RH', 'MANAGER', 'ADMIN'].includes(auth.role))
const canAct = computed(() => auth.role === 'RH')
function formatDate(value) { return value ? new Date(value).toLocaleString() : '' }
async function openFile(path) {
  // Open the tab synchronously so the browser does not block it as a popup;
  // download through Axios so the JWT interceptor can authorize the request.
  const preview = window.open('about:blank', '_blank')
  if (!preview) { error.value = 'Autorisez les fenêtres pop-up pour afficher le document.'; return }
  preview.opener = null
  error.value = ''
  try {
    const apiPath = path.replace(/^\/api(?=\/)/, '')
    const response = await api.get(apiPath, { responseType: 'blob' })
    const url = URL.createObjectURL(response.data)
    preview.location.href = url
    window.setTimeout(() => URL.revokeObjectURL(url), 60_000)
  } catch (err) {
    preview.close()
    error.value = formatApiError(err)
  }
}
async function load() {
  try {
    candidature.value = (await api.get(`/candidatures/${route.params.id}`)).data
    evaluations.value = (await api.get(`/candidatures/${route.params.id}/evaluations`)).data
  } catch (err) { error.value = formatApiError(err) }
}
async function decide(decision) {
  if (decision === 'REJETER' && !commentaire.value.trim()) { error.value = 'Le commentaire est obligatoire pour rejeter.'; return }
  try { await api.post(`/candidatures/${route.params.id}/decision-dossier`, { decision, commentaire: commentaire.value }); commentaire.value = ''; await load() } catch (err) { error.value = formatApiError(err) }
}
async function promote() {
  try { await api.post(`/candidatures/${route.params.id}/promouvoir`); await load() } catch (err) { error.value = formatApiError(err) }
}
async function schedule() {
  try { await api.post(`/candidatures/${route.params.id}/evaluations`, evaluation); evaluation.date = ''; evaluation.evaluateurId = null; await load() } catch (err) { error.value = formatApiError(err) }
}
async function result(item) {
  try { await api.put(`/evaluations/${item.id}/resultat`, { date: item.resultDate, note: item.note, remarque: item.remarque, resultat: item.resultatInput }); item.remarque = ''; await load() } catch (err) { error.value = formatApiError(err) }
}
async function finalDecision(decision) {
  if (!finalComment.value.trim()) { error.value = 'Le commentaire est obligatoire.'; return }
  try { await api.post(`/candidatures/${route.params.id}/decision-finale`, { decision, commentaire: finalComment.value }); finalComment.value = ''; await load() } catch (err) { error.value = formatApiError(err) }
}
onMounted(load)
</script>
