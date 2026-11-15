<template>
  <section class="page">
    <div class="page-heading">
      <div>
        <h2>Mes demandes</h2>
        <p class="muted">Suivi des demandes de recrutement.</p>
      </div>
      <router-link class="button primary" to="/demandes/nouvelle">Nouvelle demande</router-link>
    </div>

    <div v-if="error" class="alert error">{{ error }}</div>

    <div class="card toolbar">
      <label>
        Statut
        <select v-model="filters.statut" @change="refresh">
          <option value="">Tous</option>
          <option v-for="statut in DEMANDE_STATUTS" :key="statut" :value="statut">
            {{ statutLabel(statut) }}
          </option>
        </select>
      </label>
    </div>

    <div class="card table-wrap">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Poste</th>
            <th>Département</th>
            <th>Type</th>
            <th>Nombre</th>
            <th>Statut</th>
            <th>Date souhaitée</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in page.content" :key="item.id">
            <td>#{{ item.id }}</td>
            <td>{{ item.posteTitre }}</td>
            <td>{{ item.departementNom }}</td>
            <td>{{ typeLabel(item.type) }}</td>
            <td>{{ item.nombrePostes }}</td>
            <td><span class="status-text">{{ statutLabel(item.statut) }}</span></td>
            <td>{{ item.dateSouhaitee }}</td>
            <td class="row-actions">
              <router-link class="link-button" :to="`/demandes/${item.id}`">Détail</router-link>
              <router-link
                v-if="item.statut === 'BROUILLON'"
                class="link-button"
                :to="{ name: 'demande-nouvelle', query: { edit: item.id } }"
              >
                Modifier
              </router-link>
            </td>
          </tr>
          <tr v-if="page.content.length === 0">
            <td colspan="8" class="empty">Aucune demande.</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="pagination">
      <button class="button" :disabled="page.number === 0" @click="changePage(page.number - 1)">
        Précédent
      </button>
      <span>Page {{ page.number + 1 }} / {{ Math.max(page.totalPages, 1) }}</span>
      <button
        class="button"
        :disabled="page.number + 1 >= page.totalPages"
        @click="changePage(page.number + 1)"
      >
        Suivant
      </button>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import api from '@/api/axios'
import { formatApiError } from '@/utils/errors'
import { DEMANDE_STATUTS, statutLabel, typeLabel } from '@/utils/demande'

const filters = reactive({ statut: '' })
const error = ref('')
const page = ref({
  content: [],
  number: 0,
  totalPages: 0
})

async function fetchPage(pageNumber = 0) {
  try {
    const params = { page: pageNumber, size: 10 }
    if (filters.statut) params.statut = filters.statut
    const { data } = await api.get('/demandes', { params })
    page.value = data
  } catch (err) {
    error.value = formatApiError(err)
  }
}

function changePage(index) {
  fetchPage(index)
}

function refresh() {
  fetchPage(0)
}

onMounted(() => fetchPage(0))
</script>
