<template>
  <section class="page">
    <div class="page-heading">
      <div>
        <h2>Départements</h2>
        <p class="muted">Création, modification et suppression.</p>
      </div>
      <button class="button primary" @click="openCreate">Nouveau</button>
    </div>

    <div v-if="error" class="alert error">{{ error }}</div>

    <form v-if="isFormVisible" class="card form-grid" @submit.prevent="save">
      <label>
        Nom
        <input v-model.trim="form.nom" maxlength="100" required />
      </label>
      <label>
        Description
        <textarea v-model="form.description" rows="3" />
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
            <th>Nom</th>
            <th>Description</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in departements" :key="item.id">
            <td>{{ item.nom }}</td>
            <td>{{ item.description || '—' }}</td>
            <td class="row-actions">
              <button class="link-button" @click="openEdit(item)">Modifier</button>
              <button class="danger-link" @click="remove(item)">Supprimer</button>
            </td>
          </tr>
          <tr v-if="departements.length === 0">
            <td colspan="3" class="empty">Aucun département.</td>
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
const isFormVisible = ref(false)
const editedId = ref(null)
const error = ref('')
const form = reactive({
  nom: '',
  description: ''
})

async function fetchDepartements() {
  try {
    const { data } = await api.get('/departements')
    departements.value = data
  } catch (err) {
    error.value = formatApiError(err)
  }
}

function openCreate() {
  editedId.value = null
  form.nom = ''
  form.description = ''
  isFormVisible.value = true
  error.value = ''
}

function openEdit(item) {
  editedId.value = item.id
  form.nom = item.nom
  form.description = item.description || ''
  isFormVisible.value = true
  error.value = ''
}

function closeForm() {
  isFormVisible.value = false
  editedId.value = null
}

async function save() {
  try {
    if (editedId.value) {
      await api.put(`/departements/${editedId.value}`, form)
    } else {
      await api.post('/departements', form)
    }
    closeForm()
    await fetchDepartements()
  } catch (err) {
    error.value = formatApiError(err)
  }
}

async function remove(item) {
  if (!window.confirm(`Confirmer la suppression du département "${item.nom}" ?`)) return
  try {
    await api.delete(`/departements/${item.id}`)
    await fetchDepartements()
  } catch (err) {
    error.value = formatApiError(err)
  }
}

onMounted(fetchDepartements)
</script>
