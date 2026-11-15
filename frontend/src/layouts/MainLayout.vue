<template>
  <div class="layout">
    <aside class="sidebar">
      <div class="logo">Gestion RH</div>
      <nav class="menu">
        <router-link to="/">Tableau de bord</router-link>
        <router-link v-if="auth.hasRole(['RH', 'ADMIN'])" to="/departements">Départements</router-link>
        <router-link v-if="auth.hasRole(['RH', 'ADMIN'])" to="/postes">Postes</router-link>
        <router-link v-if="auth.hasRole(['MANAGER'])" to="/demandes">Mes demandes</router-link>
        <router-link v-if="auth.hasRole(['RH', 'FINANCE', 'DIRECTION', 'ADMIN'])" to="/consultation">
          Consultation
        </router-link>
        <router-link v-if="auth.hasRole(['RH', 'FINANCE', 'DIRECTION'])" to="/a-traiter">
          À traiter
        </router-link>
        <router-link v-if="auth.hasRole(['RH', 'MANAGER', 'ADMIN'])" to="/gestion-offres">
          Offres et candidatures
        </router-link>
        <router-link v-if="auth.hasRole(['RH', 'ADMIN'])" to="/propositions">
          Propositions d'embauche
        </router-link>
        <router-link v-if="auth.hasRole(['RH', 'ADMIN', 'MANAGER'])" to="/employes">
          Employés
        </router-link>
        <router-link v-if="auth.hasRole(['RH', 'ADMIN'])" to="/demandes-modification-dossier">
          Modifications des dossiers
        </router-link>
        <router-link v-if="auth.hasRole(['EMPLOYE'])" to="/mon-dossier">Mon dossier</router-link>
        <router-link v-if="auth.hasRole(['EMPLOYE'])" to="/mon-temps">Mon temps</router-link>
        <router-link v-if="auth.hasRole(['EMPLOYE', 'MANAGER', 'RH', 'ADMIN'])" to="/conges">Congés et absences</router-link>
        <router-link v-if="auth.hasRole(['MANAGER'])" to="/mon-equipe">Mon équipe</router-link>
        <router-link v-if="auth.hasRole(['MANAGER'])" to="/temps-equipe">Temps de l'équipe</router-link>
        <router-link v-if="auth.hasRole(['RH', 'ADMIN'])" to="/gestion-temps">Gestion du temps</router-link>
        <router-link v-if="auth.hasRole(['ADMIN', 'AUDITEUR'])" to="/audit">Journal d'audit</router-link>
        <router-link v-if="auth.hasRole(['RH', 'ADMIN', 'MANAGER', 'IT', 'SERVICES_GENERAUX'])" to="/onboardings">
          Onboardings
        </router-link>
        <router-link v-if="auth.hasRole(['RH', 'ADMIN', 'MANAGER', 'IT', 'SERVICES_GENERAUX'])" to="/mes-taches-integration">
          Mes tâches d'intégration
        </router-link>
        <router-link v-if="auth.hasRole(['RH', 'ADMIN'])" to="/contrats/echeances">
          Échéances contrats
        </router-link>
      </nav>
    </aside>

    <div class="main">
      <header class="header">
        <h1>{{ pageTitle }}</h1>
        <div class="header-actions">
          <NotificationBell />
          <span>{{ auth.user?.prenom }} {{ auth.user?.nom }} ({{ auth.role }})</span>
          <button class="link-button" @click="logout">Déconnexion</button>
        </div>
      </header>
      <main class="content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import NotificationBell from '@/components/NotificationBell.vue'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const titles = {
  dashboard: 'Tableau de bord',
  departements: 'Départements',
  postes: 'Postes',
  demandes: 'Mes demandes',
  'demande-nouvelle': 'Nouvelle demande',
  'demande-detail': 'Détail de la demande',
  consultation: 'Consultation des demandes',
  'a-traiter': 'Demandes à traiter',
  offres: 'Offres et candidatures',
  'offre-candidatures': 'Candidatures de l’offre',
  'candidature-detail': 'Détail de la candidature',
  propositions: 'Propositions d’embauche',
  'proposition-detail': 'Historique de la proposition',
  employes: 'Employés',
  'demandes-modification-dossier': 'Modifications des dossiers',
  'employe-detail': 'Fiche employé',
  'mon-dossier': 'Mon dossier',
  'mon-temps': 'Mon temps',
  conges: 'Congés et absences',
  'mon-equipe': 'Mon équipe',
  'temps-equipe': "Temps de l'équipe",
  'gestion-temps': 'Gestion du temps',
  audit: "Journal d'audit",
  onboardings: 'Onboardings',
  'onboarding-detail': "Fiche d'onboarding",
  'mes-taches-integration': "Mes tâches d'intégration",
  'contrats-echeances': 'Échéances contrats'
}

const pageTitle = computed(() => titles[route.name] || 'Gestion RH')

function logout() {
  auth.logout()
  router.push({ name: 'login' })
}
</script>
