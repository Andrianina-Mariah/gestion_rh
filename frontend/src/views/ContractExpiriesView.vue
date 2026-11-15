<template>
  <section class="page">
    <div class="page-heading"><div><h2>Contrats arrivant à échéance</h2><p class="muted">Anticipez les renouvellements et conversions.</p></div></div>
    <div v-if="error" class="alert error">{{ error }}</div>
    <div class="toolbar card"><label>Échéance dans <input v-model.number="days" type="number" min="0"></label><button class="button primary" @click="load">Rechercher</button></div>
    <div class="table-wrap card"><table><thead><tr><th>Employé</th><th>Type</th><th>Fin</th><th>Salaire</th><th>Actions</th></tr></thead><tbody>
      <tr v-for="contract in contracts" :key="contract.id"><td><router-link :to="{ name: 'employe-detail', params: { id: contract.employeId } }">Employé #{{ contract.employeId }}</router-link></td><td>{{ contract.typeContrat }}</td><td>{{ contract.dateFin }}</td><td>{{ contract.salaire }}</td><td class="actions"><button class="button" @click="renew(contract)">Renouveler</button><button v-if="contract.typeContrat === 'CDD'" class="button primary" @click="convert(contract)">Convertir en CDI</button></td></tr>
      <tr v-if="!contracts.length"><td colspan="5" class="empty">Aucun contrat à échéance.</td></tr>
    </tbody></table></div>
  </section>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import api from '@/api/axios'
import { formatApiError } from '@/utils/errors'
const contracts = ref([]); const days = ref(30); const error = ref('')
async function load() { try { contracts.value = (await api.get('/contrats/echeances', { params: { jours: days.value } })).data } catch (err) { error.value = formatApiError(err) } }
async function renew(contract) { if (!window.confirm('Créer le contrat de renouvellement ?')) return; try { await api.post(`/contrats/${contract.id}/renouveler`); await load() } catch (err) { error.value = formatApiError(err) } }
async function convert(contract) { if (!window.confirm('Créer le CDI à la suite de ce CDD ?')) return; try { await api.post(`/contrats/${contract.id}/convertir-cdi`); await load() } catch (err) { error.value = formatApiError(err) } }
onMounted(load)
</script>
