<template>
  <section class="progress-card card">
    <h3>Progression</h3>
    <div class="progress-steps">
      <div v-for="step in steps" :key="step.key" :class="['progress-step', stepState(step.key)]">
        <span class="progress-dot"></span>
        <span>{{ step.label }}</span>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  statut: {
    type: String,
    required: true
  }
})

const steps = [
  { key: 'RH', label: 'RH' },
  { key: 'FINANCE', label: 'Finance' },
  { key: 'DIRECTION', label: 'Direction' },
  { key: 'RECRUTEMENT', label: 'Recrutement' }
]

const position = computed(() => ({
  BROUILLON: 0,
  EN_ATTENTE_RH: 0,
  A_CORRIGER: 0,
  ANALYSE_FINANCE: 1,
  EN_ATTENTE_DIRECTION: 2,
  VALIDE: 3,
  RECRUTEMENT_OUVERT: 4,
  REJETE: -1
}[props.statut] ?? 0))

function stepState(key) {
  if (props.statut === 'REJETE') return 'inactive'
  const index = steps.findIndex((step) => step.key === key)
  if (index < position.value) return 'done'
  if (index === position.value) return 'current'
  return 'upcoming'
}
</script>
