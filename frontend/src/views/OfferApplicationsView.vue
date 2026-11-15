<template>
  <section class="page">
    <div class="page-heading">
      <div><h2>Candidatures — {{ offre?.titreOffre }}</h2><p class="muted">Sélection, évaluations et historique.</p></div>
      <router-link class="button" :to="{ name: 'offres' }">Retour aux offres</router-link>
    </div>
    <div class="toolbar">
      <label>Statut
        <select v-model="status" @change="load">
          <option value="">Tous</option><option v-for="item in statuses" :key="item" :value="item">{{ item }}</option>
        </select>
      </label>
      <button class="button" @click="loadWaiting">Liste d'attente</button>
      <router-link class="button" :to="{ name: 'candidats-retenus', params: { id: route.params.id } }">Candidats retenus</router-link>
    </div>
    <div v-if="error" class="alert error">{{ error }}</div>
    <div class="table-wrap card">
      <table><thead><tr><th>Candidat</th><th>Email</th><th>Statut</th><th>Date</th><th></th></tr></thead>
        <tbody><tr v-for="item in page.content" :key="item.id">
          <td>{{ item.prenom }} {{ item.nom }}</td><td>{{ item.email }}</td><td>{{ item.statut }}</td>
          <td>{{ formatDate(item.dateCandidature) }}</td>
          <td><router-link class="button" :to="{ name: 'candidature-detail', params: { id: item.id } }">Détail</router-link></td>
        </tr><tr v-if="!page.content?.length"><td colspan="5" class="empty">Aucune candidature.</td></tr></tbody>
      </table>
    </div>
    <div class="pagination"><button class="button" :disabled="page.number === 0" @click="load(page.number - 1)">Précédent</button>
      <span>Page {{ (page.number || 0) + 1 }} / {{ page.totalPages || 1 }}</span>
      <button class="button" :disabled="page.last" @click="load((page.number || 0) + 1)">Suivante</button>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import api from '@/api/axios'
import { formatApiError } from '@/utils/errors'
const route = useRoute()
const offre = ref(null)
const page = ref({ content: [], number: 0, totalPages: 0, last: true })
const status = ref('')
const error = ref('')
const waiting = ref([])
const statuses = ['RECU', 'PRESELECTIONNE', 'EN_ATTENTE', 'REJETE', 'TEST_PLANIFIE', 'TEST_PASSE', 'ENTRETIEN_PLANIFIE', 'ENTRETIEN_PASSE', 'RETENU']
const firstRetainedId = computed(() => page.value.content?.find((item) => item.statut === 'RETENU')?.id)
async function load(number = 0) {
  try {
    offre.value = (await api.get(`/offres/${route.params.id}`)).data
    const params = { page: number, size: 10 }
    if (status.value) params.statut = status.value
    page.value = (await api.get(`/offres/${route.params.id}/candidatures`, { params })).data
  } catch (err) { error.value = formatApiError(err) }
}
async function loadWaiting() {
  try { waiting.value = (await api.get(`/offres/${route.params.id}/candidatures/liste-attente`)).data; status.value = 'EN_ATTENTE'; page.value = { content: waiting.value, number: 0, totalPages: 1, last: true } } catch (err) { error.value = formatApiError(err) }
}
function formatDate(value) { return value ? new Date(value).toLocaleString() : '' }
onMounted(load)
</script>
