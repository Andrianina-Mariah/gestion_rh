<template>
  <section>
    <div class="heading">
      <h2>{{ title }}</h2>
      <button type="button" @click="loadItems">Actualiser</button>
    </div>

    <p v-if="loading">Chargement...</p>
    <p v-else-if="error" class="error">{{ error }}</p>
    <p v-else-if="items.length === 0">Aucun élément trouvé.</p>

    <table v-else>
      <thead>
        <tr>
          <th v-for="column in columns" :key="column.key">{{ column.label }}</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in items" :key="item.id">
          <td v-for="column in columns" :key="column.key">
            {{ item[column.key] ?? '—' }}
          </td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import api from '@/api/axios'

const props = defineProps({
  title: { type: String, required: true },
  endpoint: { type: String, required: true },
  columns: { type: Array, required: true }
})

const items = ref([])
const loading = ref(false)
const error = ref('')

async function loadItems() {
  loading.value = true
  error.value = ''

  try {
    const response = await api.get(props.endpoint)
    items.value = response.data
  } catch (requestError) {
    error.value = requestError.response?.status === 401
      ? 'Vous devez être connecté pour consulter cette page.'
      : 'Impossible de charger les données.'
  } finally {
    loading.value = false
  }
}

onMounted(loadItems)
</script>

<style scoped>
.heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1rem;
}

button {
  border: 0;
  border-radius: 6px;
  background: #2563eb;
  color: white;
  cursor: pointer;
  padding: 0.55rem 0.9rem;
}

table {
  width: 100%;
  border-collapse: collapse;
  background: white;
}

th,
td {
  border-bottom: 1px solid #e2e8f0;
  padding: 0.75rem;
  text-align: left;
}

th {
  background: #f1f5f9;
}

.error {
  color: #b91c1c;
}
</style>
