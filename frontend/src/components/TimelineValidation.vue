<template>
  <section class="timeline card">
    <div class="section-heading">
      <h3>Historique des décisions</h3>
      <span v-if="!items.length" class="muted">Aucune décision enregistrée.</span>
    </div>
    <ol v-if="items.length">
      <li v-for="item in items" :key="item.id">
        <div class="timeline-marker"></div>
        <div>
          <strong>{{ item.etape }} — {{ statutLabel(item.statut) }}</strong>
          <div class="muted">
            {{ item.validateurNom }} · cycle {{ item.cycle }} · {{ formatDate(item.dateValidation) }}
          </div>
          <p v-if="item.commentaire">{{ item.commentaire }}</p>
          <p v-if="item.budgetValide !== null && item.budgetValide !== undefined">
            Budget validé : {{ formatAriary(item.budgetValide) }}
          </p>
        </div>
      </li>
    </ol>
  </section>
</template>

<script setup>
import { statutLabel } from '@/utils/demande'
import { formatAriary } from '@/utils/formatMoney'

defineProps({
  items: {
    type: Array,
    default: () => []
  }
})

function formatDate(value) {
  return value ? new Date(value).toLocaleString('fr-FR') : 'Date inconnue'
}
</script>
