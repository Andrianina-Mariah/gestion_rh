<template>
  <section class="page">
    <div class="page-heading"><h2>Candidats retenus</h2><router-link class="button" :to="{ name: 'offre-candidatures', params: { id: route.params.id } }">Retour</router-link></div>
    <div v-if="error" class="alert error">{{ error }}</div>
    <div class="table-wrap card"><table><thead><tr><th>Candidat</th><th>Email</th><th>Profil</th><th>Statut</th><th></th></tr></thead>
      <tbody><tr v-for="item in candidates" :key="item.id"><td>{{ item.prenom }} {{ item.nom }}</td><td>{{ item.email }}</td><td>{{ item.titreProfil }}</td><td>{{ item.statut }}</td><td><router-link class="button" :to="{ name: 'candidature-detail', params: { id: item.id } }">Détail</router-link></td></tr>
      <tr v-if="!candidates.length"><td colspan="5" class="empty">Aucun candidat retenu.</td></tr></tbody>
    </table></div>
  </section>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import api from '@/api/axios'
import { formatApiError } from '@/utils/errors'
const route = useRoute(); const candidates = ref([]); const error = ref('')
onMounted(async () => { try { candidates.value = (await api.get(`/offres/${route.params.id}/candidats-retenus`)).data } catch (err) { error.value = formatApiError(err) } })
</script>
