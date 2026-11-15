<template>
  <section class="page">
    <div class="page-heading">
      <div>
        <h2>Consultation des demandes</h2>
        <p class="muted">Lecture seule pour RH, Finance et Direction.</p>
      </div>
    </div>

    <div v-if="error" class="alert error">{{ error }}</div>

    <div class="card toolbar toolbar-grid">
      <label>
        Statut
        <select v-model="filters.statut" @change="load">
          <option value="">Tous</option>
          <option v-for="statut in DEMANDE_STATUTS" :key="statut" :value="statut">
            {{ statutLabel(statut) }}
          </option>
        </select>
      </label>
      <label>
        Département
        <select v-model="filters.departementId" @change="load">
          <option value="">Tous</option>
          <option v-for="dep in departements" :key="dep.id" :value="dep.id">
            {{ dep.nom }}
          </option>
        </select>
      </label>
      <label>
        Période du
        <input v-model="filters.dateDebut" type="date" />
      </label>
      <label>
        au
        <input v-model="filters.dateFin" type="date" />
      </label>
    </div>

    <div class="card table-wrap">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Poste</th>
            <th>Département</th>
            <th>Demandeur</th>
            <th>Statut</th>
            <th>Date création</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in displayedDemandes" :key="item.id">
            <td>#{{ item.id }}</td>
            <td>{{ item.posteTitre }}</td>
            <td>{{ item.departementNom }}</td>
            <td>{{ item.demandeurNom }}</td>
            <td>{{ statutLabel(item.statut) }}</td>
            <td>{{ item.dateCreation?.slice(0, 10) }}</td>
            <td>
              <router-link class="link-button" :to="`/demandes/${item.id}`">Détail</router-link>
            </td>
          </tr>
          <tr v-if="displayedDemandes.length === 0">
            <td colspan="7" class="empty">Aucune demande pour ces filtres.</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import api from '@/api/axios'
import { formatApiError } from '@/utils/errors'
import { DEMANDE_STATUTS, statutLabel } from '@/utils/demande'

const error = ref('')
const demandes = ref([])
const departements = ref([])

const filters = reactive({
  statut: '',
  departementId: '',
  dateDebut: '',
  dateFin: ''
})

const displayedDemandes = computed(() => {
  let result = demandes.value

  if (filters.dateDebut) {
    result = result.filter((item) => item.dateCreation?.slice(0, 10) >= filters.dateDebut)
  }
  if (filters.dateFin) {
    result = result.filter((item) => item.dateCreation?.slice(0, 10) <= filters.dateFin)
  }
  return result
})

async function load() {
  try {
    const params = { size: 100 }
    if (filters.statut) params.statut = filters.statut
    if (filters.departementId) params.departementId = filters.departementId
    const { data } = await api.get('/demandes', { params })
    demandes.value = data.content || []
  } catch (err) {
    error.value = formatApiError(err)
  }
}

onMounted(async () => {
  try {
    const { data } = await api.get('/departements')
    departements.value = data
    await load()
  } catch (err) {
    error.value = formatApiError(err)
  }
})
</script>
