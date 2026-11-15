<template>
  <main class="public-page">
    <div class="page-heading">
      <div><h1>Offres d'emploi</h1><p class="muted">Consultez les opportunités actuellement ouvertes.</p></div>
      <router-link class="button" to="/login">Espace interne</router-link>
    </div>
    <div v-if="error" class="alert error">{{ error }}</div>
    <div class="offer-grid">
      <article v-for="offre in offres" :key="offre.id" class="card offer-card">
        <h2>{{ offre.titreOffre }}</h2>
        <p class="muted">{{ offre.lieuTravail || 'Lieu à définir' }} · {{ offre.typeContratPropose || 'Contrat à définir' }}</p>
        <p class="offer-meta"><strong>{{ offre.nombrePostes }} poste{{ offre.nombrePostes === 1 ? '' : 's' }} à pourvoir</strong><span>Budget : {{ formatAriary(offre.budget) }}</span></p>
        <p>{{ offre.descriptionPoste }}</p>
        <router-link class="button primary" :to="{ name: 'public-offre-detail', params: { id: offre.id } }">
          Voir l'offre
        </router-link>
      </article>
    </div>
    <p v-if="!offres.length && !error" class="empty">Aucune offre publiée.</p>
  </main>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import api from '@/api/axios'
import { formatApiError } from '@/utils/errors'
import { formatAriary } from '@/utils/formatMoney'

const offres = ref([])
const error = ref('')

async function load() {
  try {
    offres.value = (await api.get('/public/offres')).data
  } catch (err) {
    error.value = formatApiError(err)
  }
}
onMounted(load)
</script>
