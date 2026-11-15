<template>
  <section v-if="canAct" class="card action-validation">
    <h3>Traiter la demande</h3>
    <div v-if="error" class="alert error">{{ error }}</div>
    <label v-if="isRh" class="checkbox-label">
      <input v-model="analyseBudget" type="checkbox" />
      Demander l'analyse budget
    </label>
    <label v-if="isFinance">
      Budget validé
      <input v-model.number="budgetValide" type="number" min="0.01" step="0.01" :placeholder="String(demande.budget)" />
    </label>
    <label>
      Commentaire
      <textarea v-model="commentaire" rows="4" placeholder="Commentaire de décision"></textarea>
    </label>
    <div class="actions">
      <button v-if="isRh && analyseBudget" class="button" :disabled="loading" @click="submit('DEMANDE_ANALYSE')">
        Demander l'analyse budget
      </button>
      <button class="button primary" :disabled="loading" @click="submit('ACCEPTE')">Accepter</button>
      <button class="button" :disabled="loading" @click="submit('A_CORRIGER')">
        Demander une correction
      </button>
      <button class="danger-button" :disabled="loading" @click="submit('REJETE')">Rejeter</button>
    </div>
  </section>
</template>

<script setup>
import { computed, ref } from 'vue'
import api from '@/api/axios'
import { formatApiError } from '@/utils/errors'

const props = defineProps({
  demande: { type: Object, required: true }
})
const emit = defineEmits(['updated'])
const commentaire = ref('')
const analyseBudget = ref(false)
const budgetValide = ref(props.demande.budget)
const loading = ref(false)
const error = ref('')

const role = JSON.parse(localStorage.getItem('user') || 'null')?.role
const isRh = computed(() => role === 'RH' && props.demande.statut === 'EN_ATTENTE_RH')
const isFinance = computed(() => role === 'FINANCE' && props.demande.statut === 'ANALYSE_FINANCE')
const isDirection = computed(() => role === 'DIRECTION' && props.demande.statut === 'EN_ATTENTE_DIRECTION')
const canAct = computed(() => isRh.value || isFinance.value || isDirection.value)

async function submit(decision) {
  if (!window.confirm(`Confirmer la décision « ${decision} » ?`)) return
  loading.value = true
  error.value = ''
  try {
    const endpoint = isRh.value ? 'valider-rh' : isFinance.value ? 'valider-finance' : 'valider-direction'
    const payload = {
      decision,
      commentaire: commentaire.value,
      analyseBudget: isRh.value ? analyseBudget.value : undefined,
      budgetValide: isFinance.value ? Number(budgetValide.value) : undefined
    }
    const { data } = await api.post(`/demandes/${props.demande.id}/${endpoint}`, payload)
    commentaire.value = ''
    analyseBudget.value = false
    emit('updated', data)
  } catch (err) {
    error.value = formatApiError(err)
  } finally {
    loading.value = false
  }
}
</script>
