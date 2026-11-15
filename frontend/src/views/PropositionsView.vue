<template>
  <section class="page">
    <div class="page-heading">
      <div><h2>Propositions d'embauche</h2><p class="muted">Candidats retenus et suivi des réponses.</p></div>
    </div>
    <div v-if="error" class="alert error">{{ error }}</div>
    <div v-if="createdLink" class="card success-card">
      Proposition créée. Lien sécurisé à transmettre au candidat :
      <a :href="createdLink" target="_blank">{{ createdLink }}</a>
    </div>
    <div class="toolbar card">
      <label>Filtrer par statut
        <select v-model="statusFilter"><option value="">Tous</option><option>EN_ATTENTE</option><option>NEGOCIATION</option><option>ACCEPTE</option><option>REFUSE</option></select>
      </label>
    </div>
    <div class="table-wrap card">
      <table><thead><tr><th>Candidat</th><th>Offre</th><th>Version</th><th>Statut</th><th></th></tr></thead>
        <tbody>
          <tr v-for="item in filteredItems" :key="item.candidatureId">
            <td>{{ item.candidat }}</td><td>{{ item.offre }}</td><td>{{ item.proposition?.version || '—' }}</td>
            <td>{{ item.proposition?.statut || 'AUCUNE' }}</td>
            <td class="row-actions">
              <button v-if="!item.proposition" class="button primary" @click="openCreate(item)">Envoyer une proposition</button>
              <router-link v-else class="button" :to="{ name: 'proposition-detail', params: { id: item.candidatureId } }">Voir l'historique</router-link>
            </td>
          </tr>
          <tr v-if="!filteredItems.length"><td colspan="5" class="empty">Aucun candidat retenu.</td></tr>
        </tbody>
      </table>
    </div>

    <form v-if="creating" class="card nested-form" @submit.prevent="createProposal">
      <h3>Envoyer une proposition à {{ creating.candidat }}</h3>
      <div class="form-grid">
        <label>Date de début <input v-model="form.dateDebut" type="date" required /></label>
        <label>Lieu <input v-model="form.lieu" required maxlength="150" /></label>
        <label>Salaire (en ariary) <input v-model.number="form.salaire" type="number" min="1" step="1" required placeholder="Ex. 500 000" /><small class="muted">Saisissez un montant numérique ; le budget de la demande est le plafond.</small></label>
        <label>Type de contrat <select v-model="form.typeContrat"><option>CE</option><option>CDD</option><option>CDI</option></select></label>
      </div>
      <div class="actions"><button class="button primary">Envoyer</button><button type="button" class="button" @click="creating = null">Annuler</button></div>
    </form>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import api from '@/api/axios'
import { formatApiError } from '@/utils/errors'

const items = ref([])
const error = ref('')
const statusFilter = ref('')
const creating = ref(null)
const createdLink = ref('')
const form = reactive({ dateDebut: '', lieu: '', salaire: null, typeContrat: 'CDI' })
const filteredItems = computed(() => statusFilter.value ? items.value.filter(item => item.proposition?.statut === statusFilter.value) : items.value)

async function load() {
  try {
    const offers = (await api.get('/offres')).data
    const rows = []
    for (const offer of offers) {
      const retained = (await api.get(`/offres/${offer.id}/candidats-retenus`)).data
      for (const candidate of retained) {
        const history = (await api.get(`/candidatures/${candidate.id}/propositions`)).data
        rows.push({ candidatureId: candidate.id, candidat: `${candidate.prenom} ${candidate.nom}`, offre: offer.titreOffre, proposition: history.at(-1) || null })
      }
    }
    items.value = rows
  } catch (err) { error.value = formatApiError(err) }
}
function openCreate(item) { creating.value = item; form.dateDebut = ''; form.lieu = ''; form.salaire = null; form.typeContrat = 'CDI' }
async function createProposal() {
  try {
    const response = await api.post(`/candidatures/${creating.value.candidatureId}/propositions`, form)
    if (response.data.jeton) {
      createdLink.value = `${window.location.origin}/proposition/${response.data.id}?token=${encodeURIComponent(response.data.jeton)}`
    }
    creating.value = null
    await load()
  } catch (err) { error.value = formatApiError(err) }
}
onMounted(load)
</script>
