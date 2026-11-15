<template>
  <section class="page">
    <div class="page-heading"><div><h2>Mon équipe</h2><p class="muted">Informations professionnelles uniquement.</p></div><button class="button" @click="load">Actualiser</button></div>
    <div v-if="error" class="alert error">{{ error }}</div>
    <div class="card table-wrap"><table><thead><tr><th>Matricule</th><th>Nom</th><th>Statut</th><th>Poste</th><th>Département</th><th></th></tr></thead><tbody><tr v-for="employee in employees" :key="employee.id"><td>{{ employee.matricule }}</td><td>{{ employee.prenom }} {{ employee.nom }}</td><td>{{ employee.statut }}</td><td>{{ employee.poste || '—' }}</td><td>{{ employee.departement || '—' }}</td><td><router-link class="button" :to="{ name: 'employe-detail', params: { id: employee.id } }">Voir</router-link></td></tr><tr v-if="!employees.length"><td colspan="6" class="empty">Aucun membre.</td></tr></tbody></table></div>
  </section>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import api from '@/api/axios'
import { formatApiError } from '@/utils/errors'
const employees = ref([]); const error = ref('')
async function load() { try { employees.value = (await api.get('/employes')).data } catch (err) { error.value = formatApiError(err) } }
onMounted(load)
</script>
