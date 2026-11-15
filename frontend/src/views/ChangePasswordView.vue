<template>
  <main class="auth-page">
    <form class="card auth-card" @submit.prevent="submit">
      <h1>Choisir votre mot de passe</h1>
      <p class="muted">Pour sécuriser votre compte, remplacez le mot de passe de première connexion par votre mot de passe personnel.</p>
      <div v-if="error" class="alert error">{{ error }}</div>
      <label>Mot de passe actuel<input v-model="form.ancienMotDePasse" type="password" autocomplete="current-password" required /></label>
      <label>Nouveau mot de passe<input v-model="form.nouveauMotDePasse" type="password" autocomplete="new-password" minlength="8" required /></label>
      <label>Confirmer le nouveau mot de passe<input v-model="confirmation" type="password" autocomplete="new-password" minlength="8" required /></label>
      <button class="button primary" :disabled="loading">{{ loading ? 'Enregistrement...' : 'Enregistrer mon mot de passe' }}</button>
    </form>
  </main>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/api/axios'
import { useAuthStore } from '@/stores/auth'
import { formatApiError } from '@/utils/errors'

const auth = useAuthStore()
const router = useRouter()
const form = reactive({ ancienMotDePasse: '', nouveauMotDePasse: '' })
const confirmation = ref('')
const loading = ref(false)
const error = ref('')

async function submit() {
  error.value = ''
  if (form.nouveauMotDePasse !== confirmation.value) {
    error.value = 'Les deux nouveaux mots de passe ne correspondent pas.'
    return
  }
  loading.value = true
  try {
    await api.post('/auth/changer-mot-de-passe', form)
    auth.setPasswordChanged()
    await router.replace('/')
  } catch (err) {
    error.value = formatApiError(err)
  } finally {
    loading.value = false
  }
}
</script>
