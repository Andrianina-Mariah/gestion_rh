<template>
  <div class="notification-box">
    <button class="notification-toggle" aria-label="Notifications" @click="open = !open">
      <span aria-hidden="true">🔔</span>
      <span v-if="unreadCount" class="notification-count">{{ unreadCount }}</span>
    </button>
    <div v-if="open" class="notification-panel">
      <div class="notification-panel-heading">
        <strong>Notifications</strong>
        <button class="link-button" @click="load">Actualiser</button>
      </div>
      <p v-if="error" class="notification-error">{{ error }}</p>
      <div v-for="notification in notifications" :key="notification.id" class="notification-item" :class="{ unread: !notification.lue }">
        <router-link v-if="notification.lien" :to="notification.lien" @click="open = false">
          {{ notification.message }}
        </router-link>
        <span v-else>{{ notification.message }}</span>
        <small>{{ formatDate(notification.dateCreation) }}</small>
        <button v-if="!notification.lue" class="link-button" @click="markRead(notification)">Marquer comme lue</button>
      </div>
      <p v-if="!notifications.length && !error" class="muted">Aucune notification.</p>
    </div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import api from '@/api/axios'
import { formatApiError } from '@/utils/errors'

const notifications = ref([])
const unreadCount = ref(0)
const open = ref(false)
const error = ref('')
let polling

async function load() {
  try {
    const [list, count] = await Promise.all([
      api.get('/notifications'),
      api.get('/notifications/non-lues/count')
    ])
    notifications.value = list.data
    unreadCount.value = count.data
    error.value = ''
  } catch (err) {
    error.value = formatApiError(err)
  }
}

async function markRead(notification) {
  try {
    await api.patch(`/notifications/${notification.id}/lue`)
    await load()
  } catch (err) {
    error.value = formatApiError(err)
  }
}

function formatDate(value) {
  return value ? new Date(value).toLocaleString('fr-FR') : ''
}

onMounted(() => {
  load()
  polling = window.setInterval(load, 45000)
})

onBeforeUnmount(() => window.clearInterval(polling))
</script>
