<template>
  <section class="page">
    <div class="page-heading"><div><h2>Onboardings</h2><p class="muted">Suivi des intégrations et de leur progression.</p></div></div>
    <div v-if="error" class="alert error">{{ error }}</div>
    <div class="toolbar card">
      <label>Statut <select v-model="filters.statut" @change="load"><option value="">Tous</option><option>A_FAIRE</option><option>EN_COURS</option><option>TERMINE</option><option>ANNULE</option></select></label>
      <label v-if="canFilterDepartment">Département <select v-model="filters.departement" @change="load"><option value="">Tous</option><option v-for="department in departments" :key="department">{{ department }}</option></select></label>
      <label>Date de début <input v-model="filters.date" type="date" @change="load" /></label>
      <button class="button primary" @click="load">Actualiser</button>
    </div>
    <div class="table-wrap card"><table><thead><tr><th>Employé</th><th>Début</th><th>Statut</th><th>Progression</th><th></th></tr></thead>
      <tbody><tr v-for="item in filtered" :key="item.id"><td>{{ item.employeNom }}<br><span class="muted">{{ item.matricule }}</span></td><td>{{ item.dateDebut }}</td><td>{{ item.statut }}</td><td><progress :value="item.progression" max="100"></progress> {{ item.progression }}%</td><td><router-link class="button" :to="{ name: 'onboarding-detail', params: { id: item.id } }">Ouvrir</router-link></td></tr>
      <tr v-if="!filtered.length"><td colspan="5" class="empty">Aucun onboarding trouvé.</td></tr></tbody>
    </table></div>
  </section>
</template>
<script setup>
import { computed, onMounted, ref } from 'vue'
import api from '@/api/axios'
import { formatApiError } from '@/utils/errors'
import { useAuthStore } from '@/stores/auth'
const auth = useAuthStore()
const items = ref([]); const employees = ref([]); const error = ref('')
const filters = ref({ statut: '', departement: '', date: '' })
const canFilterDepartment = computed(() => auth.hasRole(['RH', 'ADMIN', 'MANAGER']))
const departments = computed(() => [...new Set(employees.value.map(e => e.departement).filter(Boolean))].sort())
const filtered = computed(() => items.value.filter(item => {
  const employee = employees.value.find(e => e.id === item.employeId)
  return (!filters.value.departement || employee?.departement === filters.value.departement) &&
    (!filters.value.date || item.dateDebut === filters.value.date)
}))
async function load() {
  try {
    const onboardingResponse = await api.get('/onboardings', { params: { statut: filters.value.statut || undefined } })
    items.value = onboardingResponse.data
    if (canFilterDepartment.value) {
      const peopleResponse = await api.get('/employes')
      employees.value = peopleResponse.data
    } else {
      employees.value = []
      filters.value.departement = ''
    }
  } catch (err) { error.value = formatApiError(err) }
}
onMounted(load)
</script>
