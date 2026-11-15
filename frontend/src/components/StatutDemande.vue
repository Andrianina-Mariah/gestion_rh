<template>
  <span class="status-text">{{ statutLabel(statut) }}</span>
</template>

<script setup>
import { statutLabel } from '@/utils/demande'

defineProps({
  statut: {
    type: String,
    required: true
  }
})
</script>
