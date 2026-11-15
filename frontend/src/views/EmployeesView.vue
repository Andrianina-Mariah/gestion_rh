<template>
  <section class="page">
    <div class="page-heading"><div><h2>Dossier employés</h2><p class="muted">Matricules, contrats et affectations.</p></div></div>
    <div v-if="error" class="alert error">{{ error }}</div>
    <div class="toolbar card">
      <label>Recherche <input v-model="search" placeholder="Nom, email ou matricule" @keyup.enter="load" /></label>
      <label>Statut <select v-model="status" @change="load"><option value="">Tous</option><option>EN_ONBOARDING</option><option>ACTIF</option><option>SORTIE</option></select></label>
      <button class="button primary" @click="load">Rechercher</button>
    </div>
    <div class="table-wrap card"><table><thead><tr><th>Matricule</th><th>Identité</th><th>Email</th><th>Statut</th><th>Poste</th><th></th></tr></thead>
      <tbody><tr v-for="employee in employees" :key="employee.id"><td>{{ employee.matricule }}</td><td>{{ employee.prenom }} {{ employee.nom }}</td><td>{{ employee.email }}</td><td>{{ employee.statut }}</td><td>{{ employee.poste || '—' }}</td><td><router-link class="button" :to="{ name: 'employe-detail', params: { id: employee.id } }">Ouvrir</router-link></td></tr>
      <tr v-if="!employees.length"><td colspan="6" class="empty">Aucun employé.</td></tr></tbody>
    </table></div>
  </section>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import api from '@/api/axios'
import { formatApiError } from '@/utils/errors'
const employees = ref([]); const error = ref(''); const search = ref(''); const status = ref('')
async function load() { try { employees.value = (await api.get('/employes', { params: { q: search.value || undefined, statut: status.value || undefined } })).data } catch (err) { error.value = formatApiError(err) } }
onMounted(load)
</script>
