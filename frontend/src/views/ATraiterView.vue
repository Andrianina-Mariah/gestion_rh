<template>
  <section class="page">
    <div class="page-heading">
      <div>
        <h2>À traiter</h2>
        <p class="muted">Demandes en attente pour votre rôle.</p>
      </div>
      <button class="button" @click="load">Actualiser</button>
    </div>
    <div v-if="error" class="alert error">{{ error }}</div>
    <div class="card table-wrap">
      <table>
        <thead><tr><th>ID</th><th>Poste</th><th>Département</th><th>Demandeur</th><th>Statut</th><th>Actions</th></tr></thead>
        <tbody>
          <tr v-for="item in demandes" :key="item.id">
            <td>#{{ item.id }}</td><td>{{ item.posteTitre }}</td><td>{{ item.departementNom }}</td>
            <td>{{ item.demandeurNom }}</td><td><StatutDemande :statut="item.statut" /></td>
            <td><router-link class="link-button" :to="`/demandes/${item.id}`">Traiter</router-link></td>
          </tr>
          <tr v-if="!demandes.length"><td colspan="6" class="empty">Aucune demande à traiter.</td></tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import api from '@/api/axios'
import { formatApiError } from '@/utils/errors'
import StatutDemande from '@/components/StatutDemande.vue'

const demandes = ref([])
const error = ref('')
async function load() {
  try { demandes.value = (await api.get('/demandes/a-traiter')).data } catch (err) { error.value = formatApiError(err) }
}
onMounted(load)
</script>
