<template>
  <main class="auth-page">
    <form class="card auth-card" @submit.prevent="submit">
      <h1>Connexion</h1>
      <p class="muted">Saisissez vos identifiants.</p>

      <div v-if="error" class="alert error">{{ error }}</div>

      <label>
        Email
        <input v-model.trim="form.email" type="email" required autocomplete="username" />
      </label>
      <label>
        Mot de passe
        <input v-model="form.password" type="password" required autocomplete="current-password" />
      </label>

      <button class="button primary" :disabled="loading">
        {{ loading ? 'Connexion...' : 'Se connecter' }}
      </button>
    </form>
  </main>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { formatApiError } from '@/utils/errors'

const form = reactive({ email: '', password: '' })
const loading = ref(false)
const error = ref('')
const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

async function submit() {
  loading.value = true
  error.value = ''

  try {
    await auth.login(form)
    router.push(auth.user?.changementMotDePasseRequis ? '/changer-mot-de-passe' : (route.query.redirect || '/'))
  } catch (err) {
    error.value = formatApiError(err)
  } finally {
    loading.value = false
  }
}
</script>
