<template>
  <section class="page">
    <div class="page-heading">
      <div>
        <h2>{{ editedId ? 'Modifier un brouillon' : 'Nouvelle demande' }}</h2>
        <p class="muted">Renseignez les informations de la demande de recrutement.</p>
      </div>
    </div>

    <div v-if="error" class="alert error">{{ error }}</div>
    <div v-if="Object.keys(validationErrors).length" class="alert error">
      <ul>
        <li v-for="(message, key) in validationErrors" :key="key">{{ message }}</li>
      </ul>
    </div>

    <form class="card form-grid" @submit.prevent>
      <label>
        Département
        <select v-model="selectedDepartementId">
          <option value="" disabled>Choisir un département</option>
          <option v-for="dep in departements" :key="dep.id" :value="dep.id">{{ dep.nom }}</option>
        </select>
      </label>

      <label>
        Poste
        <select v-model="form.posteId" required>
          <option value="" disabled>Choisir un poste</option>
          <option v-for="poste in filteredPostes" :key="poste.id" :value="poste.id">
            {{ poste.titre }}
          </option>
        </select>
      </label>

      <label>
        Type
        <select v-model="form.type" required>
          <option value="" disabled>Choisir un type</option>
          <option v-for="type in DEMANDE_TYPES" :key="type" :value="type">{{ typeLabel(type) }}</option>
        </select>
      </label>

      <label>
        Nombre de postes
        <input v-model.number="form.nombrePostes" type="number" min="1" required />
      </label>

      <label>
        Motif
        <textarea v-model="form.motif" rows="4" required />
      </label>

      <label>
        Budget total (en ariary)
        <input v-model.number="form.budget" type="number" min="1" step="1" required placeholder="Ex. 1 000 000" />
        <small class="muted">Saisissez un montant numérique en ariary. Si le salaire reste à discuter, indiquez le budget plafond validé.</small>
      </label>

      <label>
        Date souhaitée
        <input v-model="form.dateSouhaitee" type="date" required />
      </label>

      <div class="actions">
        <button type="button" class="button primary" @click="saveDraft">
          {{ existingStatus === 'A_CORRIGER' ? 'Enregistrer les corrections' : 'Enregistrer en brouillon' }}
        </button>
        <button v-if="existingStatus === 'A_CORRIGER'" type="button" class="button" @click="saveAndResubmit">
          Enregistrer et resoumettre
        </button>
        <button v-if="editedId && existingStatus === 'BROUILLON'" type="button" class="button" @click="submitDraft">Soumettre</button>
        <button v-if="editedId" type="button" class="danger-button" @click="removeDraft">Supprimer</button>
      </div>
    </form>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/api/axios'
import { formatApiError } from '@/utils/errors'
import { DEMANDE_TYPES, typeLabel, validateDemande } from '@/utils/demande'

const route = useRoute()
const router = useRouter()
const editedId = ref(route.query.edit || null)
const existingStatus = ref(null)
const error = ref('')
const validationErrors = ref({})
const departements = ref([])
const postes = ref([])
const selectedDepartementId = ref('')

const form = reactive({
  posteId: '',
  type: '',
  nombrePostes: 1,
  motif: '',
  budget: '',
  dateSouhaitee: ''
})

const filteredPostes = computed(() => {
  if (!selectedDepartementId.value) return postes.value
  return postes.value.filter((poste) => poste.departementId === Number(selectedDepartementId.value))
})

async function loadReferentials() {
  const [departementRes, posteRes] = await Promise.all([
    api.get('/departements'),
    api.get('/postes')
  ])
  departements.value = departementRes.data
  postes.value = posteRes.data
}

async function loadExistingDemande() {
  if (!editedId.value) return
  const { data } = await api.get(`/demandes/${editedId.value}`)
  if (data.statut !== 'BROUILLON') {
    if (data.statut !== 'A_CORRIGER') {
      router.replace(`/demandes/${editedId.value}`)
      return
    }
  }
  existingStatus.value = data.statut
  form.posteId = data.posteId
  form.type = data.type
  form.nombrePostes = data.nombrePostes
  form.motif = data.motif
  form.budget = data.budget
  form.dateSouhaitee = data.dateSouhaitee
  const poste = postes.value.find((item) => item.id === data.posteId)
  selectedDepartementId.value = poste?.departementId || ''
}

function payload() {
  return {
    posteId: Number(form.posteId),
    type: form.type,
    nombrePostes: Number(form.nombrePostes),
    motif: form.motif,
    budget: Number(form.budget),
    dateSouhaitee: form.dateSouhaitee
  }
}

function isValid() {
  validationErrors.value = validateDemande(form)
  return Object.keys(validationErrors.value).length === 0
}

async function saveDraft(event) {
  event.preventDefault()
  if (!isValid()) return

  error.value = ''
  try {
    if (editedId.value) {
      await api.put(`/demandes/${editedId.value}`, payload())
      router.push(`/demandes/${editedId.value}`)
    } else {
      const { data } = await api.post('/demandes', payload())
      router.push(`/demandes/${data.id}`)
    }
  } catch (err) {
    error.value = formatApiError(err)
  }
}

async function submitDraft(event) {
  event.preventDefault()
  if (!editedId.value) return
  error.value = ''

  try {
    await api.post(`/demandes/${editedId.value}/soumettre`)
    router.push(`/demandes/${editedId.value}`)
  } catch (err) {
    error.value = formatApiError(err)
  }
}

async function saveAndResubmit(event) {
  event.preventDefault()
  if (!editedId.value || existingStatus.value !== 'A_CORRIGER' || !isValid()) return
  error.value = ''
  try {
    await api.put(`/demandes/${editedId.value}`, payload())
    await api.post(`/demandes/${editedId.value}/resoumettre`)
    router.push(`/demandes/${editedId.value}`)
  } catch (err) {
    error.value = formatApiError(err)
  }
}

async function removeDraft() {
  if (!editedId.value) return
  if (!window.confirm('Confirmer la suppression de ce brouillon ?')) return

  try {
    await api.delete(`/demandes/${editedId.value}`)
    router.push('/demandes')
  } catch (err) {
    error.value = formatApiError(err)
  }
}

onMounted(async () => {
  try {
    await loadReferentials()
    await loadExistingDemande()
  } catch (err) {
    error.value = formatApiError(err)
  }
})
</script>
