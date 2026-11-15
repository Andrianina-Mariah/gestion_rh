<template>
  <main class="public-page">
    <div class="page-heading">
      <div>
        <h1>Proposition d'embauche</h1>
        <p class="muted">Consultez les conditions et choisissez une réponse.</p>
      </div>
    </div>

    <div v-if="error" class="alert error">{{ error }}</div>
    <div v-if="success" class="card success-card">{{ success }}</div>

    <section v-if="proposal && !success" class="card">
      <div class="detail-grid">
        <div><strong>Version :</strong> {{ proposal.version }}</div>
        <div><strong>Statut :</strong> {{ proposal.statut }}</div>
        <div><strong>Date de début :</strong> {{ formatDate(proposal.dateDebut) }}</div>
        <div><strong>Lieu :</strong> {{ proposal.lieu }}</div>
        <div><strong>Salaire :</strong> {{ formatMoney(proposal.salaire) }}</div>
        <div><strong>Type de contrat :</strong> {{ proposal.typeContrat }}</div>
      </div>

      <form class="nested-form" @submit.prevent="respond">
        <label>Remarque
          <textarea v-model="form.remarque" rows="3" placeholder="Votre remarque (facultative)" />
        </label>
        <label v-if="form.reponse === 'NEGOCIER'">Souhaits
          <textarea v-model="form.souhaits" rows="3" placeholder="Précisez vos souhaits" />
        </label>
        <div class="actions">
          <button class="button primary" type="submit" @click="form.reponse = 'ACCEPTER'">Accepter</button>
          <button class="button" type="submit" @click="form.reponse = 'NEGOCIER'">Négocier</button>
          <button class="danger-button" type="submit" @click="form.reponse = 'REFUSER'">Refuser</button>
        </div>
      </form>
    </section>

    <section v-if="history.length" class="card">
      <h2>Versions précédentes</h2>
      <div v-for="item in history" :key="item.id" class="evaluation-row">
        <strong>Version {{ item.version }}</strong> — {{ item.statut }} —
        {{ formatDate(item.dateProposition) }}
        <span v-if="item.remarqueCandidat"> — {{ item.remarqueCandidat }}</span>
      </div>
    </section>
  </main>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import api from '@/api/axios'
import { formatApiError } from '@/utils/errors'
import { formatAriary } from '@/utils/formatMoney'

const route = useRoute()
const proposal = ref(null)
const history = ref([])
const error = ref('')
const success = ref('')
const form = reactive({ reponse: 'ACCEPTER', remarque: '', souhaits: '' })
const token = route.query.token || ''

const formatDate = (value) => value ? new Date(value).toLocaleDateString() : '—'
const formatMoney = formatAriary

async function load() {
  try {
    const response = await api.get(`/public/propositions/${route.params.id}`, { params: { jeton: token } })
    proposal.value = response.data
    history.value = (await api.get(`/public/propositions/${route.params.id}/versions`, { params: { jeton: token } })).data
  } catch (err) {
    error.value = formatApiError(err)
  }
}

async function respond() {
  if (!token) {
    error.value = 'Le lien sécurisé est incomplet.'
    return
  }
  try {
    await api.post(`/propositions/${route.params.id}/repondre`, { jeton: token, ...form })
    success.value = 'Votre réponse a bien été enregistrée.'
    proposal.value = { ...proposal.value, statut: form.reponse === 'ACCEPTER' ? 'ACCEPTE' : form.reponse === 'REFUSER' ? 'REFUSE' : 'NEGOCIATION' }
  } catch (err) {
    error.value = formatApiError(err)
  }
}

onMounted(load)
</script>
