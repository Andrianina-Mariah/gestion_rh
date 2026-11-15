<template>
  <main class="public-page">
    <router-link class="button" to="/offres">← Toutes les offres</router-link>
    <div v-if="error" class="alert error">{{ error }}</div>
    <template v-if="offre">
      <section class="card">
        <h1>{{ offre.titreOffre }}</h1>
        <p class="muted">{{ offre.lieuTravail }} · {{ offre.typeContratPropose }}</p>
        <p class="offer-meta"><strong>{{ offre.nombrePostes }} poste{{ offre.nombrePostes === 1 ? '' : 's' }} à pourvoir</strong><span>Budget : {{ formatAriary(offre.budget) }}</span></p>
        <h2>Description</h2><p class="preformatted">{{ offre.descriptionPoste }}</p>
        <h2>Profil recherché</h2><p class="preformatted">{{ offre.profilRecherche }}</p>
      </section>
      <section v-if="sent" class="card success-card">
        <h2>Candidature envoyée</h2>
        <p>Votre dossier a bien été transmis. Merci pour votre candidature.</p>
        <router-link class="button" to="/offres">Retour aux offres</router-link>
      </section>
      <section v-else class="card">
        <h2>Postuler</h2>
        <div class="stepper"><strong>Étape {{ step }}/4</strong> — {{ stepLabels[step - 1] }}</div>
        <div v-if="formError" class="alert error">{{ formError }}</div>
        <form @submit.prevent="next">
          <div v-if="step === 1" class="form-grid">
            <label>Nom <input v-model="form.nom" required /></label>
            <label>Prénom <input v-model="form.prenom" required /></label>
            <label>Email <input v-model="form.email" type="email" required /></label>
            <label>Téléphone <input v-model="form.telephone" /></label>
            <label class="full">Adresse <input v-model="form.adresse" /></label>
          </div>
          <div v-else-if="step === 2" class="form-grid">
            <label>Titre du profil <input v-model="form.titreProfil" required /></label>
            <label>Langues <input v-model="form.langues" /></label>
            <label class="full">Résumé <textarea v-model="form.resume" rows="4" /></label>
            <label class="full">Compétences <textarea v-model="form.competences" rows="4" /></label>
          </div>
          <div v-else-if="step === 3">
            <div v-for="(item, index) in form.experiences" :key="index" class="nested-form">
              <div class="form-grid">
                <label>Poste <input v-model="item.poste" /></label>
                <label>Entreprise <input v-model="item.entreprise" /></label>
                <label>Date début <input v-model="item.dateDebut" type="date" /></label>
                <label>Date fin <input v-model="item.dateFin" type="date" /></label>
                <label class="full">Description <textarea v-model="item.description" rows="2" /></label>
              </div>
              <button type="button" class="danger-button" @click="removeItem(form.experiences, index)">Supprimer</button>
            </div>
            <button type="button" class="button" @click="addExperience">+ Ajouter une expérience</button>
          </div>
          <div v-else class="form-grid">
            <div class="full">
              <h3>Formations</h3>
              <div v-for="(item, index) in form.formations" :key="index" class="nested-form">
                <label>Diplôme <input v-model="item.diplome" /></label>
                <label>Établissement <input v-model="item.etablissement" /></label>
                <label>Année <input v-model.number="item.anneeObtention" type="number" /></label>
                <button type="button" class="danger-button" @click="removeItem(form.formations, index)">Supprimer</button>
              </div>
              <button type="button" class="button" @click="addFormation">+ Ajouter une formation</button>
            </div>
            <label>CV (PDF, 5 Mo max) <input ref="cv" type="file" accept="application/pdf" required /></label>
            <label>Lettre (PDF, DOC ou DOCX) <input ref="letter" type="file" accept=".pdf,.doc,.docx" /></label>
          </div>
          <div class="actions">
            <button v-if="step > 1" type="button" class="button" @click="step--">Précédent</button>
            <button class="button primary" :disabled="loading">{{ step === 4 ? 'Envoyer ma candidature' : 'Suivant' }}</button>
          </div>
        </form>
      </section>
    </template>
  </main>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '@/api/axios'
import { formatApiError } from '@/utils/errors'
import { formatAriary } from '@/utils/formatMoney'

const route = useRoute()
const offre = ref(null)
const error = ref('')
const formError = ref('')
const step = ref(1)
const loading = ref(false)
const sent = ref(false)
const cv = ref(null)
const letter = ref(null)
const stepLabels = ['Identité', 'Profil et compétences', 'Expériences', 'Formations et pièces jointes']
const form = reactive({
  nom: '', prenom: '', email: '', telephone: '', adresse: '', titreProfil: '',
  resume: '', competences: '', langues: '', experiences: [], formations: []
})

function addExperience() {
  form.experiences.push({ poste: '', entreprise: '', dateDebut: '', dateFin: '', enPoste: false, description: '' })
}
function addFormation() {
  form.formations.push({ diplome: '', etablissement: '', anneeObtention: null, description: '' })
}
function removeItem(list, index) { list.splice(index, 1) }

async function next() {
  formError.value = ''
  if (step.value < 4) {
    step.value++
    return
  }
  if (!cv.value?.files?.[0]) {
    formError.value = 'Le CV PDF est obligatoire.'
    return
  }
  loading.value = true
  try {
    const data = new FormData()
    data.append('candidature', new Blob([JSON.stringify(form)], { type: 'application/json' }))
    data.append('cv', cv.value.files[0])
    if (letter.value?.files?.[0]) data.append('lettreMotivation', letter.value.files[0])
    await api.post(`/public/offres/${route.params.id}/candidatures`, data, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    sent.value = true
  } catch (err) {
    formError.value = formatApiError(err)
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  try { offre.value = (await api.get(`/public/offres/${route.params.id}`)).data } catch (err) { error.value = formatApiError(err) }
})
</script>
