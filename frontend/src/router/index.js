import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '@/layouts/MainLayout.vue'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/offres',
    name: 'public-offres',
    component: () => import('@/views/PublicOffersView.vue')
  },
  {
    path: '/offres/:id',
    name: 'public-offre-detail',
    component: () => import('@/views/PublicOfferDetailView.vue')
  },
  {
    path: '/proposition/:id',
    name: 'public-proposition',
    component: () => import('@/views/PublicProposalView.vue')
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { guestOnly: true }
  },
  {
    path: '/changer-mot-de-passe',
    name: 'changer-mot-de-passe',
    component: () => import('@/views/ChangePasswordView.vue'),
    meta: { requiresAuth: true, passwordChangeOnly: true }
  },
  {
    path: '/',
    component: MainLayout,
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'dashboard',
        component: () => import('@/views/DashboardView.vue')
      },
      {
        path: 'departements',
        name: 'departements',
        component: () => import('@/views/DepartementsView.vue'),
        meta: { roles: ['RH', 'ADMIN'] }
      },
      {
        path: 'postes',
        name: 'postes',
        component: () => import('@/views/PostesView.vue'),
        meta: { roles: ['RH', 'ADMIN'] }
      },
      {
        path: 'demandes',
        name: 'demandes',
        component: () => import('@/views/DemandesView.vue'),
        meta: { roles: ['MANAGER'] }
      },
      {
        path: 'demandes/nouvelle',
        name: 'demande-nouvelle',
        component: () => import('@/views/DemandeFormView.vue'),
        meta: { roles: ['MANAGER'] }
      },
      {
        path: 'demandes/:id',
        name: 'demande-detail',
        component: () => import('@/views/DemandeDetailView.vue'),
        meta: { roles: ['MANAGER', 'RH', 'FINANCE', 'DIRECTION', 'ADMIN'] }
      },
      {
        path: 'consultation',
        name: 'consultation',
        component: () => import('@/views/ConsultationView.vue'),
        meta: { roles: ['RH', 'FINANCE', 'DIRECTION', 'ADMIN'] }
      },
      {
        path: 'a-traiter',
        name: 'a-traiter',
        component: () => import('@/views/ATraiterView.vue'),
        meta: { roles: ['RH', 'FINANCE', 'DIRECTION'] }
      },
      {
        path: 'gestion-offres',
        name: 'offres',
        component: () => import('@/views/OffersView.vue'),
        meta: { roles: ['RH', 'MANAGER', 'ADMIN'] }
      },
      {
        path: 'gestion-offres/:id/candidatures',
        name: 'offre-candidatures',
        component: () => import('@/views/OfferApplicationsView.vue'),
        meta: { roles: ['RH', 'MANAGER', 'ADMIN'] }
      },
      {
        path: 'gestion-offres/:id/candidats-retenus',
        name: 'candidats-retenus',
        component: () => import('@/views/RetainedCandidatesView.vue'),
        meta: { roles: ['RH', 'MANAGER', 'ADMIN'] }
      },
      {
        path: 'gestion-candidatures/:id',
        name: 'candidature-detail',
        component: () => import('@/views/CandidatureDetailView.vue'),
        meta: { roles: ['RH', 'MANAGER', 'ADMIN'] }
      },
      {
        path: 'propositions',
        name: 'propositions',
        component: () => import('@/views/PropositionsView.vue'),
        meta: { roles: ['RH', 'ADMIN'] }
      },
      {
        path: 'propositions/:id',
        name: 'proposition-detail',
        component: () => import('@/views/PropositionDetailView.vue'),
        meta: { roles: ['RH', 'ADMIN'] }
      },
      {
        path: 'employes',
        name: 'employes',
        component: () => import('@/views/EmployeesView.vue'),
        meta: { roles: ['RH', 'ADMIN', 'MANAGER'] }
      },
      {
        path: 'demandes-modification-dossier',
        name: 'demandes-modification-dossier',
        component: () => import('@/views/EmployeeChangesView.vue'),
        meta: { roles: ['RH', 'ADMIN'] }
      },
      {
        path: 'employes/:id',
        name: 'employe-detail',
        component: () => import('@/views/EmployeeDetailView.vue'),
        meta: { roles: ['RH', 'ADMIN', 'MANAGER', 'EMPLOYE'] }
      },
      {
        path: 'mon-dossier',
        name: 'mon-dossier',
        component: () => import('@/views/MyDossierView.vue'),
        meta: { roles: ['EMPLOYE'] }
      },
      {
        path: 'mon-temps',
        name: 'mon-temps',
        component: () => import('@/views/MonTempsView.vue'),
        meta: { roles: ['EMPLOYE'] }
      },
      {
        path: 'conges',
        name: 'conges',
        component: () => import('@/views/CongesView.vue'),
        meta: { roles: ['EMPLOYE', 'MANAGER', 'RH', 'ADMIN'] }
      },
      {
        path: 'temps-equipe',
        name: 'temps-equipe',
        component: () => import('@/views/TempsEquipeView.vue'),
        meta: { roles: ['MANAGER'] }
      },
      {
        path: 'gestion-temps',
        name: 'gestion-temps',
        component: () => import('@/views/GestionTempsView.vue'),
        meta: { roles: ['RH', 'ADMIN'] }
      },
      {
        path: 'mon-equipe',
        name: 'mon-equipe',
        component: () => import('@/views/TeamView.vue'),
        meta: { roles: ['MANAGER'] }
      },
      {
        path: 'audit',
        name: 'audit',
        component: () => import('@/views/AuditView.vue'),
        meta: { roles: ['ADMIN', 'AUDITEUR'] }
      },
      {
        path: 'onboardings',
        name: 'onboardings',
        component: () => import('@/views/OnboardingsView.vue'),
        meta: { roles: ['RH', 'ADMIN', 'MANAGER', 'IT', 'SERVICES_GENERAUX'] }
      },
      {
        path: 'onboardings/:id',
        name: 'onboarding-detail',
        component: () => import('@/views/OnboardingDetailView.vue'),
        meta: { roles: ['RH', 'ADMIN', 'MANAGER', 'IT', 'SERVICES_GENERAUX'] }
      },
      {
        path: 'mes-taches-integration',
        name: 'mes-taches-integration',
        component: () => import('@/views/MyOnboardingTasksView.vue'),
        meta: { roles: ['RH', 'ADMIN', 'MANAGER', 'IT', 'SERVICES_GENERAUX'] }
      },
      {
        path: 'contrats/echeances',
        name: 'contrats-echeances',
        component: () => import('@/views/ContractExpiriesView.vue'),
        meta: { roles: ['RH', 'ADMIN'] }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()

  if (auth.isAuthenticated && !auth.user) {
    try {
      await auth.loadUser()
    } catch (error) {
      auth.logout()
    }
  }

  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (to.meta.guestOnly && auth.isAuthenticated) {
    return auth.user?.changementMotDePasseRequis
      ? { name: 'changer-mot-de-passe' }
      : { name: 'dashboard' }
  }

  if (auth.isAuthenticated && auth.user?.changementMotDePasseRequis && !to.meta.passwordChangeOnly) {
    return { name: 'changer-mot-de-passe' }
  }
  if (to.meta.passwordChangeOnly && !auth.user?.changementMotDePasseRequis) {
    return { name: 'dashboard' }
  }

  if (to.meta.roles && !auth.hasRole(to.meta.roles)) {
    return { name: 'dashboard' }
  }

  return true
})

export default router
