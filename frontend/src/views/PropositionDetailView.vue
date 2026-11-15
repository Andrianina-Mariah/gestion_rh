<template>
  <section class="page">
    <div class="page-heading"><div><h2>Historique de la proposition</h2><p class="muted">Versions et négociation.</p></div><router-link class="button" to="/propositions">Retour</router-link></div>
    <div v-if="error" class="alert error">{{ error }}</div>
    <section v-if="newProposalLink" class="card success-card">
      <h3>Nouvelle proposition prête à transmettre</h3>
      <p>Copiez et envoyez ce lien au candidat. Il donne accès à la version révisée.</p>
      <a :href="newProposalLink" target="_blank" rel="noreferrer">{{ newProposalLink }}</a>
      <button class="button" type="button" @click="copyLink">Copier le lien</button>
      <p v-if="linkCopied" class="muted">Lien copié.</p>
    </section>
    <section v-for="item in proposals" :key="item.id" class="card">
      <div class="page-heading"><h3>Version {{ item.version }}</h3><span>{{ item.statut }}</span></div>
      <div class="detail-grid">
        <div><strong>Date de début :</strong> {{ item.dateDebut }}</div><div><strong>Lieu :</strong> {{ item.lieu }}</div>
        <div><strong>Salaire :</strong> {{ formatAriary(item.salaire) }}</div><div><strong>Contrat :</strong> {{ item.typeContrat }}</div>
        <div class="full"><strong>Remarque candidat :</strong> {{ item.remarqueCandidat || '—' }}</div>
        <div class="full"><strong>Souhaits :</strong> {{ item.souhaitsCandidat || '—' }}</div>
      </div>
      <div v-if="item.id === latest?.id && item.statut === 'NEGOCIATION'" class="actions">
        <button class="button primary" @click="showAdjust = true">Ajuster</button>
        <button class="danger-button" @click="reject">Rejeter</button>
      </div>
    </section>
    <form v-if="showAdjust" class="card nested-form" @submit.prevent="adjust">
      <h3>Nouvelle version</h3>
      <div class="form-grid"><label>Date de début <input v-model="form.dateDebut" type="date" required /></label><label>Lieu <input v-model="form.lieu" required /></label><label>Salaire (en ariary) <input v-model.number="form.salaire" type="number" min="1" step="1" required placeholder="Ex. 500 000" /><small class="muted">Saisissez un montant numérique ; le budget de la demande est le plafond.</small></label><label>Type <select v-model="form.typeContrat"><option>CE</option><option>CDD</option><option>CDI</option></select></label></div>
      <button class="button primary">Créer la version</button>
    </form>
  </section>
</template>
<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import api from '@/api/axios'
import { formatApiError } from '@/utils/errors'
import { formatAriary } from '@/utils/formatMoney'
const route = useRoute(); const proposals = ref([]); const error = ref(''); const showAdjust = ref(false)
const newProposalLink = ref(''); const linkCopied = ref(false)
const form = reactive({ dateDebut: '', lieu: '', salaire: null, typeContrat: 'CDI' })
const latest = computed(() => proposals.value.at(-1))
async function load() { try { proposals.value = (await api.get(`/candidatures/${route.params.id}/propositions`)).data } catch (err) { error.value = formatApiError(err) } }
async function adjust() {
  try {
    const { data } = await api.post(`/propositions/${latest.value.id}/ajuster`, form)
    if (data.jeton) newProposalLink.value = `${window.location.origin}/proposition/${data.id}?token=${encodeURIComponent(data.jeton)}`
    linkCopied.value = false
    showAdjust.value = false
    form.dateDebut = ''; form.lieu = ''; form.salaire = null; form.typeContrat = 'CDI'
    await load()
  } catch (err) { error.value = formatApiError(err) }
}
async function copyLink() {
  try { await navigator.clipboard.writeText(newProposalLink.value); linkCopied.value = true }
  catch { linkCopied.value = false; error.value = 'Copiez le lien affiché manuellement.' }
}
async function reject() { if (!window.confirm('Rejeter cette négociation ?')) return; try { await api.post(`/propositions/${latest.value.id}/rejeter`); await load() } catch (err) { error.value = formatApiError(err) } }
onMounted(load)
</script>
