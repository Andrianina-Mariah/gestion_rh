<template>
  <section class="page">
    <div class="page-heading"><div><h2>Demandes de modification du dossier</h2><p class="muted">Demandes envoyées par les salariés et en attente de décision RH.</p></div><button class="button" @click="load">Actualiser</button></div>
    <div v-if="error" class="alert error">{{ error }}</div>
    <div class="table-wrap card"><table><thead><tr><th>Salarié</th><th>Champ</th><th>Valeur actuelle</th><th>Valeur demandée</th><th>Commentaire du salarié</th><th>Décision</th></tr></thead>
      <tbody><tr v-for="request in requests" :key="request.id"><td>{{ employeeName(request.employeId) }}</td><td>{{ fieldName(request.champ) }}</td><td>{{ request.ancienneValeur || '—' }}</td><td>{{ request.nouvelleValeur }}</td><td>{{ request.commentaire || '—' }}</td><td><div class="row-actions"><button class="button primary" :disabled="busyId === request.id" @click="decide(request, true)">Approuver</button><button class="danger-link" :disabled="busyId === request.id" @click="decide(request, false)">Rejeter</button></div></td></tr>
      <tr v-if="!requests.length"><td colspan="6" class="empty">Aucune demande de modification en attente.</td></tr></tbody>
    </table></div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import api from '@/api/axios'
import { formatApiError } from '@/utils/errors'

const requests = ref([])
const employees = ref([])
const error = ref('')
const busyId = ref(null)

function employeeName(id) {
  const employee = employees.value.find(item => Number(item.id) === Number(id))
  return employee ? `${employee.prenom} ${employee.nom} · ${employee.matricule}` : `Employé #${id}`
}
function fieldName(field) {
  return ({ telephone: 'Téléphone', adresse: 'Adresse', nationalite: 'Nationalité', situationFamiliale: 'Situation familiale' })[field] || field
}
async function load() {
  error.value = ''
  try {
    const [requestResponse, employeeResponse] = await Promise.all([api.get('/demandes-modification'), api.get('/employes')])
    requests.value = requestResponse.data
    employees.value = employeeResponse.data
  } catch (err) { error.value = formatApiError(err) }
}
async function decide(request, approve) {
  let commentaire = ''
  if (!approve) {
    commentaire = window.prompt('Motif du rejet :')
    if (commentaire === null) return
    if (!commentaire.trim()) { error.value = 'Le motif du rejet est obligatoire.'; return }
  }
  busyId.value = request.id
  error.value = ''
  try {
    await api.post(`/demandes-modification/${request.id}/${approve ? 'approuver' : 'rejeter'}`, { commentaire })
    await load()
  } catch (err) { error.value = formatApiError(err) } finally { busyId.value = null }
}
onMounted(load)
</script>
