<template>
  <section class="page">
    <div class="page-heading">
      <div>
        <h2>Postes</h2>
        <p class="muted">Gestion des postes avec filtre par département.</p>
      </div>
      <button class="button primary" @click="openCreate">Nouveau</button>
    </div>

    <div v-if="error" class="alert error">{{ error }}</div>

    <div class="card toolbar">
      <label>
        Filtre département
        <select v-model="departementFilter" @change="fetchPostes">
          <option value="">Tous</option>
          <option v-for="dep in departements" :key="dep.id" :value="dep.id">
            {{ dep.nom }}
          </option>
        </select>
      </label>
    </div>

    <form v-if="isFormVisible" class="card form-grid" @submit.prevent="save">
      <label>
        Titre
        <input v-model.trim="form.titre" maxlength="150" required />
      </label>
      <label>
        Département
        <select v-model="form.departementId" required>
          <option value="" disabled>Choisir un département</option>
          <option v-for="dep in departements" :key="dep.id" :value="dep.id">
            {{ dep.nom }}
          </option>
        </select>
      </label>
      <label>
        Description du profil
        <textarea v-model="form.descriptionProfil" rows="3" />
      </label>
      <div class="actions">
        <button class="button primary">{{ editedId ? 'Modifier' : 'Créer' }}</button>
        <button type="button" class="button" @click="closeForm">Annuler</button>
      </div>
    </form>

    <div class="card table-wrap">
      <table>
        <thead>
          <tr>
            <th>Titre</th>
            <th>Département</th>
            <th>Description</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in postes" :key="item.id">
            <td>{{ item.titre }}</td>
            <td>{{ item.departementNom }}</td>
            <td>{{ item.descriptionProfil || '—' }}</td>
            <td class="row-actions">
              <button class="link-button" @click="openEdit(item)">Modifier</button>
              <button class="danger-link" @click="remove(item)">Supprimer</button>
            </td>
          </tr>
          <tr v-if="postes.length === 0">
            <td colspan="4" class="empty">Aucun poste.</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import api from '@/api/axios'
import { formatApiError } from '@/utils/errors'

const departements = ref([])
const postes = ref([])
const departementFilter = ref('')
const isFormVisible = ref(false)
const editedId = ref(null)
const error = ref('')

const form = reactive({
  titre: '',
  departementId: '',
  descriptionProfil: ''
})

async function fetchDepartements() {
  const { data } = await api.get('/departements')
  departements.value = data
}

async function fetchPostes() {
  try {
    const params = departementFilter.value ? { departementId: departementFilter.value } : {}
    const { data } = await api.get('/postes', { params })
    postes.value = data
  } catch (err) {
    error.value = formatApiError(err)
  }
}

function openCreate() {
  editedId.value = null
  form.titre = ''
  form.departementId = ''
  form.descriptionProfil = ''
  isFormVisible.value = true
  error.value = ''
}

function openEdit(item) {
  editedId.value = item.id
  form.titre = item.titre
  form.departementId = item.departementId
  form.descriptionProfil = item.descriptionProfil || ''
  isFormVisible.value = true
  error.value = ''
}

function closeForm() {
  editedId.value = null
  isFormVisible.value = false
}

async function save() {
  try {
    if (editedId.value) {
      await api.put(`/postes/${editedId.value}`, form)
    } else {
      await api.post('/postes', form)
    }
    closeForm()
    await fetchPostes()
  } catch (err) {
    error.value = formatApiError(err)
  }
}

async function remove(item) {
  if (!window.confirm(`Confirmer la suppression du poste "${item.titre}" ?`)) return
  try {
    await api.delete(`/postes/${item.id}`)
    await fetchPostes()
  } catch (err) {
    error.value = formatApiError(err)
  }
}

onMounted(async () => {
  try {
    await fetchDepartements()
    await fetchPostes()
  } catch (err) {
    error.value = formatApiError(err)
  }
})
</script>
