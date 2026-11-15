import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import api from '@/api/axios'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token'))
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))

  const isAuthenticated = computed(() => Boolean(token.value))
  const role = computed(() => user.value?.role || null)

  async function login(credentials) {
    const { data } = await api.post('/auth/login', credentials)
    token.value = data.token
    user.value = {
      id: data.id,
      email: data.email,
      nom: data.nom,
      prenom: data.prenom,
      role: data.role,
      changementMotDePasseRequis: Boolean(data.changementMotDePasseRequis)
    }
    localStorage.setItem('token', data.token)
    localStorage.setItem('user', JSON.stringify(user.value))
  }

  async function loadUser() {
    if (!token.value) return
    const { data } = await api.get('/auth/me')
    user.value = data
    localStorage.setItem('user', JSON.stringify(data))
  }

  function logout() {
    token.value = null
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  function setPasswordChanged() {
    if (!user.value) return
    user.value.changementMotDePasseRequis = false
    localStorage.setItem('user', JSON.stringify(user.value))
  }

  function hasRole(roles) {
    return roles.includes(role.value)
  }

  return {
    token,
    user,
    role,
    isAuthenticated,
    login,
    loadUser,
    logout,
    setPasswordChanged,
    hasRole
  }
})
