<template>
  <section class="page">
    <div class="page-heading">
      <div><h2>Offres d'emploi</h2><p class="muted">Gérez les offres et accédez aux candidatures.</p></div>
      <router-link class="button" to="/offres">Voir l'espace public</router-link>
    </div>
    <div v-if="error" class="alert error">{{ error }}</div>
    <div class="offer-grid">
      <article v-for="offre in offres" :key="offre.id" class="card offer-card">
        <template v-if="editing === offre.id">
          <div class="form-grid">
            <label class="full">Titre <input v-model="edit.titreOffre" /></label>
            <label class="full">Description <textarea v-model="edit.descriptionPoste" rows="4" /></label>
            <label class="full">Profil <textarea v-model="edit.profilRecherche" rows="4" /></label>
            <label>Lieu <input v-model="edit.lieuTravail" /></label>
            <label>Contrat <input v-model="edit.typeContratPropose" /></label>
          </div>
          <div class="actions">
            <button class="button primary" @click="save(offre.id)">Enregistrer</button>
            <button class="button" @click="editing = null">Annuler</button>
          </div>
        </template>
        <template v-else>
          <h3>{{ offre.titreOffre }}</h3>
          <p class="muted">{{ offre.statut }} · {{ offre.lieuTravail || 'Lieu à définir' }} · {{ offre.typeContratPropose || 'Contrat à définir' }}</p>
          <p class="offer-meta"><strong>{{ offre.nombrePostes }} poste{{ offre.nombrePostes === 1 ? '' : 's' }} à pourvoir</strong><span>Budget : {{ formatAriary(offre.budget) }}</span></p>
          <p>{{ offre.descriptionPoste }}</p>
          <div class="actions">
            <router-link class="button" :to="{ name: 'offre-candidatures', params: { id: offre.id } }">Candidatures</router-link>
            <button v-if="offre.statut === 'BROUILLON' && auth.role === 'RH'" class="button" @click="startEdit(offre)">Modifier</button>
            <button v-if="offre.statut === 'BROUILLON' && auth.role === 'RH'" class="button primary" @click="publish(offre.id)">Publier</button>
            <button v-if="offre.statut === 'PUBLIEE' && auth.role === 'RH'" class="button" @click="close(offre.id)">Clôturer</button>
            <button class="button" @click="share(offre.id)">Partager</button>
            <button class="button" @click="print(offre)">Imprimer</button>
          </div>
          <p v-if="shareMessages[offre.id]" class="muted offer-share-link">{{ shareMessages[offre.id] }}</p>
        </template>
      </article>
    </div>
    <p v-if="!offres.length" class="empty">Aucune offre accessible.</p>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import api from '@/api/axios'
import { formatApiError } from '@/utils/errors'
import { formatAriary } from '@/utils/formatMoney'

const auth = useAuthStore()
const offres = ref([])
const error = ref('')
const editing = ref(null)
const shareMessages = ref({})
const edit = reactive({})

async function load() {
  try { offres.value = (await api.get('/offres')).data } catch (err) { error.value = formatApiError(err) }
}
function startEdit(offre) {
  editing.value = offre.id
  Object.assign(edit, offre)
}
async function save(id) {
  try { await api.put(`/offres/${id}`, edit); editing.value = null; await load() } catch (err) { error.value = formatApiError(err) }
}
async function publish(id) {
  if (!window.confirm('Publier cette offre ?')) return
  try { await api.post(`/offres/${id}/publier`); await load() } catch (err) { error.value = formatApiError(err) }
}
async function close(id) {
  if (!window.confirm('Clôturer cette offre ? Les candidatures en cours seront rejetées.')) return
  try { await api.post(`/offres/${id}/cloturer`); await load() } catch (err) { error.value = formatApiError(err) }
}
async function share(id) {
  const link = `${window.location.origin}/offres/${id}`
  try { await navigator.clipboard.writeText(link); shareMessages.value = { ...shareMessages.value, [id]: `Lien copié : ${link}` } }
  catch { shareMessages.value = { ...shareMessages.value, [id]: `Lien : ${link}` } }
}
function print(offre) {
  const popup = window.open('', '_blank')
  if (!popup) { error.value = 'Autorisez les fenêtres pop-up pour imprimer l’offre.'; return }
  const budget = formatAriary(offre.budget)
  popup.document.write(`<h1>${offre.titreOffre}</h1><p>${offre.descriptionPoste}</p><h2>Profil recherché</h2><p>${offre.profilRecherche}</p>`)
  popup.document.write(`<p>${offre.nombrePostes} poste(s) à pourvoir · Budget : ${budget}</p>`)
  popup.document.close(); popup.print()
}
onMounted(load)
</script>
