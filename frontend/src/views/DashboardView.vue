<template>
  <section class="page">
    <div class="page-heading">
      <div>
        <h2>Tableau de bord</h2>
        <p class="muted">Vue synthétique des demandes récentes.</p>
      </div>
      <button class="button" @click="load">Actualiser</button>
    </div>
    <div v-if="error" class="alert error">{{ error }}</div>

    <div v-if="showCounters" class="stat-cards">
      <div v-for="stat in counters" :key="stat.key" class="card stat-card">
        <span class="muted">{{ stat.label }}</span>
        <strong>{{ stat.value }}</strong>
      </div>
    </div>

    <section class="card table-wrap">
      <div class="section-heading"><h3>10 dernières demandes</h3></div>
      <table>
        <thead><tr><th>ID</th><th>Poste</th><th>Demandeur</th><th>Statut</th><th>Date</th><th></th></tr></thead>
        <tbody>
          <tr v-for="item in latest" :key="item.id">
            <td>#{{ item.id }}</td><td>{{ item.posteTitre }}</td><td>{{ item.demandeurNom }}</td>
            <td><StatutDemande :statut="item.statut" /></td><td>{{ item.dateCreation?.slice(0, 10) }}</td>
            <td><router-link class="link-button" :to="`/demandes/${item.id}`">Voir</router-link></td>
          </tr>
          <tr v-if="!latest.length"><td colspan="6" class="empty">Aucune demande.</td></tr>
        </tbody>
      </table>
    </section>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import api from '@/api/axios'
import { formatApiError } from '@/utils/errors'
import { DEMANDE_STATUTS, statutLabel } from '@/utils/demande'
import { useAuthStore } from '@/stores/auth'
import StatutDemande from '@/components/StatutDemande.vue'

const auth = useAuthStore()
const latest = ref([])
const all = ref([])
const error = ref('')
const showCounters = computed(() => auth.hasRole(['RH', 'DIRECTION']))
const counters = computed(() => DEMANDE_STATUTS.map((key) => ({
  key,
  label: statutLabel(key),
  value: all.value.filter((item) => item.statut === key).length
})))

async function load() {
  try {
    const { data } = await api.get('/demandes', { params: { size: 100, page: 0 } })
    all.value = data.content || []
    latest.value = [...all.value]
      .sort((a, b) => String(b.dateCreation).localeCompare(String(a.dateCreation)))
      .slice(0, 10)
  } catch (err) {
    error.value = formatApiError(err)
  }
}

onMounted(load)
</script>
