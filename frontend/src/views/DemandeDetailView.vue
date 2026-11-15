<template>
  <section class="page">
    <div class="page-heading">
      <div>
        <h2>Détail de la demande #{{ demande?.id }}</h2>
        <p class="muted">Informations, progression et décisions.</p>
      </div>
      <div class="actions">
        <router-link
          v-if="demande?.statut === 'BROUILLON' && auth.hasRole(['MANAGER'])"
          class="button"
          :to="{ name: 'demande-nouvelle', query: { edit: demande.id } }"
        >
          Modifier le brouillon
        </router-link>
        <router-link
          v-if="demande?.statut === 'A_CORRIGER' && auth.hasRole(['MANAGER'])"
          class="button primary"
          :to="{ name: 'demande-nouvelle', query: { edit: demande.id } }"
        >
          Corriger et resoumettre
        </router-link>
      </div>
    </div>

    <div v-if="error" class="alert error">{{ error }}</div>

    <template v-if="demande">
      <div class="card detail-grid">
        <div><strong>Poste :</strong> {{ demande.posteTitre }}</div>
        <div><strong>Département :</strong> {{ demande.departementNom }}</div>
        <div><strong>Demandeur :</strong> {{ demande.demandeurNom }}</div>
        <div><strong>Type :</strong> {{ typeLabel(demande.type) }}</div>
        <div><strong>Statut :</strong> <StatutDemande :statut="demande.statut" /></div>
        <div><strong>Nombre de postes :</strong> {{ demande.nombrePostes }}</div>
        <div><strong>Budget :</strong> {{ formatAriary(demande.budget) }}</div>
        <div><strong>Date souhaitée :</strong> {{ demande.dateSouhaitee }}</div>
        <div class="full"><strong>Motif :</strong> {{ demande.motif }}</div>
      </div>

      <ProgressValidation :statut="demande.statut" />

      <div v-if="lastCorrection" class="alert correction">
        <strong>Correction demandée :</strong> {{ lastCorrection.commentaire }}
      </div>

      <ActionValidation v-if="canValidate" :demande="demande" @updated="handleUpdated" />

      <section v-if="canOpenRecruitment" class="card action-validation">
        <h3>Ouverture du recrutement</h3>
        <p class="muted">La validation finale est terminée. La RH peut lancer le recrutement.</p>
        <button class="button primary" :disabled="loading" @click="openRecruitment">
          Ouvrir le recrutement
        </button>
      </section>

      <TimelineValidation :items="demande.historique || []" />
    </template>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/api/axios'
import { formatApiError } from '@/utils/errors'
import { statutLabel, typeLabel } from '@/utils/demande'
import { useAuthStore } from '@/stores/auth'
import ActionValidation from '@/components/ActionValidation.vue'
import ProgressValidation from '@/components/ProgressValidation.vue'
import StatutDemande from '@/components/StatutDemande.vue'
import TimelineValidation from '@/components/TimelineValidation.vue'
import { formatAriary } from '@/utils/formatMoney'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const demande = ref(null)
const error = ref('')
const loading = ref(false)

const canValidate = computed(() => ['RH', 'FINANCE', 'DIRECTION'].includes(auth.role))
const canOpenRecruitment = computed(() => auth.role === 'RH' && demande.value?.statut === 'VALIDE')
const lastCorrection = computed(() => [...(demande.value?.historique || [])].reverse()
  .find((item) => item.statut === 'A_CORRIGER'))

async function load() {
  try {
    const { data } = await api.get(`/demandes/${route.params.id}`)
    demande.value = data
  } catch (err) {
    error.value = formatApiError(err)
  }
}

function handleUpdated(updated) {
  demande.value = { ...demande.value, ...updated }
  load()
}

async function openRecruitment() {
  if (!window.confirm('Confirmer l’ouverture du recrutement ?')) return
  loading.value = true
  error.value = ''
  try {
    await api.post(`/demandes/${demande.value.id}/ouvrir-recrutement`)
    await load()
  } catch (err) {
    error.value = formatApiError(err)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
