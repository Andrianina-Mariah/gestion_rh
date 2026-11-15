<template>
  <section class="page">
    <div class="page-heading"><div><h2>Journal d'audit</h2><p class="muted">Consultation réservée à l'administration et à l'audit.</p></div><button class="button" @click="exportCsv">Exporter la vue</button></div>
    <div v-if="error" class="alert error">{{ error }}</div>
    <form class="card toolbar-grid" @submit.prevent="load">
      <label>Utilisateur <input v-model="filters.utilisateurId" type="number"></label>
      <label>Entité <input v-model="filters.entite"></label>
      <label>Action <select v-model="filters.action"><option value="">Toutes</option><option>LECTURE</option><option>CREATION</option><option>MODIFICATION</option><option>SUPPRESSION</option><option>EXPORT</option></select></label>
      <label>Depuis <input v-model="filters.depuis" type="datetime-local"></label>
      <label>Jusqu'à <input v-model="filters.jusqua" type="datetime-local"></label>
      <button class="button primary">Filtrer</button>
    </form>
    <div class="card table-wrap"><table><thead><tr><th>Date</th><th>Utilisateur</th><th>Rôle</th><th>Action</th><th>Entité</th><th>ID</th><th>Détail</th></tr></thead>
      <tbody><tr v-for="row in rows" :key="row.id"><td>{{ row.dateHeure?.replace('T', ' ') }}</td><td>{{ row.utilisateurId || 'Système' }}</td><td>{{ row.role || '—' }}</td><td>{{ row.action }}</td><td>{{ row.entite }}</td><td>{{ row.entiteId || '—' }}</td><td>{{ row.nouvelleValeur || '—' }}</td></tr><tr v-if="!rows.length"><td colspan="7" class="empty">Aucun événement.</td></tr></tbody>
    </table></div>
  </section>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue'
import api from '@/api/axios'
import { formatApiError } from '@/utils/errors'
const rows = ref([]); const error = ref('')
const filters = reactive({ utilisateurId: '', entite: '', action: '', depuis: '', jusqua: '' })
async function load() { try { rows.value = (await api.get('/audit', { params: Object.fromEntries(Object.entries(filters).filter(([, value]) => value)) })).data } catch (err) { error.value = formatApiError(err) } }
function exportCsv() {
  const header = 'date,utilisateur,role,action,entite,entiteId\n'
  const body = rows.value.map(r => [r.dateHeure, r.utilisateurId, r.role, r.action, r.entite, r.entiteId].map(v => `"${String(v ?? '').replaceAll('"', '""')}"`).join(',')).join('\n')
  const link = document.createElement('a'); link.href = URL.createObjectURL(new Blob([header + body], { type: 'text/csv;charset=utf-8' })); link.download = 'journal-audit.csv'; link.click(); URL.revokeObjectURL(link.href)
}
onMounted(load)
</script>
