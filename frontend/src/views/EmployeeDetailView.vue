<template>
  <section class="page">
    <div class="page-heading"><div><h2>Fiche dossier salarié</h2><p class="muted">Consultation selon vos habilitations.</p></div><router-link class="button" to="/employes">Retour</router-link></div>
    <div v-if="error" class="alert error">{{ error }}</div>
    <template v-if="dossier">
      <section class="card detail-grid"><div><strong>Matricule :</strong> {{ dossier.identite.matricule }}</div><div><strong>Statut :</strong> {{ dossier.identite.statut }}</div><div><strong>Identité :</strong> {{ dossier.identite.prenom }} {{ dossier.identite.nom }}</div><div><strong>Poste :</strong> {{ dossier.identite.poste || '—' }}</div><div><strong>Département :</strong> {{ dossier.identite.departement || '—' }}</div><div><strong>Ancienneté :</strong> {{ dossier.anciennete }}</div></section>
      <nav class="tabs"><button v-for="tab in tabs" :key="tab.key" class="button" :class="{ primary: active === tab.key }" @click="active = tab.key">{{ tab.label }}</button></nav>
      <section v-if="active === 'identite'" class="card detail-grid"><div><strong>Email :</strong> {{ dossier.identite.email }}</div><div><strong>Téléphone :</strong> {{ dossier.identite.telephone || '—' }}</div><div><strong>Date de naissance :</strong> {{ dossier.identite.dateNaissance || '—' }}</div><div><strong>Nationalité :</strong> {{ dossier.identite.nationalite || '—' }}</div><div><strong>CIN :</strong> {{ dossier.identite.cin || 'Masqué' }}</div><div><strong>CNaPS :</strong> {{ dossier.identite.numeroCnaps || 'Masqué' }}</div></section>
      <section v-if="active === 'coordonnees'" class="card"><h3>Coordonnées</h3><div v-for="address in dossier.adresses" :key="address.id" class="evaluation-row"><strong>{{ address.type }}</strong><span>{{ address.adresse }} {{ address.ville || '' }}</span></div><div v-for="contact in dossier.contacts" :key="contact.id" class="evaluation-row"><strong>{{ contact.type }}</strong><span>{{ contact.valeur }}</span></div></section>
      <section v-if="active === 'situation'" class="card">
        <h3>Situation professionnelle</h3>
        <h4>Affectations</h4>
        <div v-for="assignment in dossier.affectations" :key="assignment.id" class="evaluation-row"><strong>{{ assignment.poste }} — {{ assignment.departement }}</strong><span>{{ assignment.dateAffectation }} → {{ assignment.dateFin || 'en cours' }}<template v-if="assignment.managerId"> · Manager #{{ assignment.managerId }}</template></span></div>
        <div v-if="!dossier.affectations?.length" class="empty">Aucune affectation enregistrée.</div>
        <form v-if="canManageEmployeeData" class="card form-grid nested-card" @submit.prevent="saveAssignment">
          <h4 class="full">Enregistrer une affectation ou une mutation</h4>
          <label>Département <select v-model.number="assignmentForm.departementId" required @change="loadPosts"><option disabled value="">Sélectionner</option><option v-for="department in departments" :key="department.id" :value="department.id">{{ department.nom }}</option></select></label>
          <label>Poste <select v-model.number="assignmentForm.posteId" required><option disabled value="">Sélectionner</option><option v-for="post in availablePosts" :key="post.id" :value="post.id">{{ post.titre }}</option></select></label>
          <label>Manager (facultatif) <select v-model="assignmentForm.managerId"><option value="">Aucun</option><option v-for="manager in managerOptions" :key="manager.id" :value="String(manager.id)">{{ manager.prenom }} {{ manager.nom }} · #{{ manager.id }}</option></select></label>
          <label>Date d’effet <input v-model="assignmentForm.dateAffectation" type="date" required></label>
          <div class="actions full"><button class="button primary" :disabled="saving">Enregistrer l’affectation</button></div>
        </form>
        <template v-if="canSeeSalary">
          <h4>Historique des salaires</h4>
          <div v-for="salary in dossier.salaires" :key="salary.id" class="evaluation-row"><strong>{{ formatMoney(salary.montant, salary.devise) }}</strong><span>{{ salary.dateDebut }} → {{ salary.dateFin || 'en cours' }}<template v-if="salary.motif"> · {{ salary.motif }}</template></span></div>
          <div v-if="!dossier.salaires?.length" class="empty">Aucun historique de salaire enregistré.</div>
          <form v-if="canManageEmployeeData" class="card form-grid nested-card" @submit.prevent="saveSalary">
            <h4 class="full">Enregistrer un changement de salaire</h4>
            <label>Montant (Ar) <input v-model="salaryForm.montant" type="number" min="0.01" step="0.01" required></label>
            <label>Date d’effet <input v-model="salaryForm.dateDebut" type="date" required></label>
            <label>Contrat associé <select v-model="salaryForm.contratId"><option value="">Aucun</option><option v-for="contract in dossier.contrats" :key="contract.id" :value="String(contract.id)">#{{ contract.id }} · {{ contract.typeContrat }} ({{ contract.statut }})</option></select></label>
            <label>Motif <input v-model="salaryForm.motif" placeholder="Ex. Révision annuelle" required></label>
            <div class="actions full"><button class="button primary" :disabled="saving">Enregistrer le salaire</button></div>
          </form>
        </template>
      </section>
      <section v-if="active === 'contrats'" class="card"><h3>Contrats successifs</h3><div v-for="contract in dossier.contrats" :key="contract.id" class="evaluation-row"><strong>{{ contract.typeContrat }} — {{ contract.statut }}</strong><span>{{ contract.dateDebut }} → {{ contract.dateFin || 'indéterminé' }} · {{ contract.salaire }}</span><div v-if="canManageContracts" class="actions"><button v-if="['BROUILLON', 'CREE'].includes(contract.statut)" class="button" :disabled="contractBusy" @click="signContract(contract)">Enregistrer la signature / le dépôt</button><button v-if="contract.statut === 'SIGNE'" class="button primary" :disabled="contractBusy" @click="validateContract(contract)">Valider le contrat (RH)</button><button v-if="contract.statut === 'VALIDE'" class="button" :disabled="contractBusy" @click="resetEmployeeAccess(contract)">Réinitialiser l’accès salarié</button></div></div><div v-if="!dossier.contrats?.length" class="empty">Aucun contrat enregistré pour cet employé.</div></section>
      <section v-if="active === 'carriere'" class="card"><h3>Frise de carrière</h3><div class="timeline"><div v-for="event in career" :key="`${event.type}-${event.dateDebut}`" class="evaluation-row"><strong>{{ event.dateDebut }} · {{ event.type }}</strong><span>{{ event.libelle }} {{ event.details || '' }}</span></div></div></section>
      <section v-if="active === 'documents'" class="card"><h3>Documents</h3><div v-for="doc in dossier.documents" :key="doc.id" class="evaluation-row"><strong>{{ doc.type }} v{{ doc.version }}</strong><span>{{ doc.statut }} · expiration {{ doc.dateExpiration || 'sans expiration' }}</span><div class="actions"><button class="button" @click="download(doc)">Télécharger</button><button v-if="canManageDocuments && doc.statut === 'DEPOSE'" class="button primary" @click="decide(doc, true)">Valider</button><button v-if="canManageDocuments && doc.statut === 'DEPOSE'" class="danger-button" @click="decide(doc, false)">Rejeter</button></div></div></section>
      <section v-if="active === 'audit'" class="card"><h3>Audit de ce salarié</h3><div v-for="row in auditRows" :key="row.id" class="evaluation-row"><strong>{{ row.dateHeure?.replace('T', ' ') }}</strong><span>{{ row.action }} {{ row.entite }} #{{ row.entiteId || '' }}</span></div><div v-if="!auditRows.length" class="empty">Aucun événement visible.</div></section>
    </template>
  </section>
</template>
<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import api from '@/api/axios'
import { formatApiError } from '@/utils/errors'
const route = useRoute(); const auth = useAuthStore(); const dossier = ref(null); const career = ref([]); const auditRows = ref([]); const active = ref('identite'); const error = ref(''); const contractBusy = ref(false); const saving = ref(false); const departments = ref([]); const posts = ref([]); const employees = ref([])
const today = new Date().toISOString().slice(0, 10)
const assignmentForm = reactive({ departementId: '', posteId: '', managerId: '', dateAffectation: today })
const salaryForm = reactive({ montant: '', devise: 'MGA', dateDebut: today, motif: '', contratId: '' })
const tabs = computed(() => [{ key: 'identite', label: 'Identité' }, { key: 'coordonnees', label: 'Coordonnées' }, { key: 'situation', label: 'Situation professionnelle' }, { key: 'contrats', label: 'Contrats' }, { key: 'carriere', label: 'Carrière' }, { key: 'documents', label: 'Documents' }, ...(auth.hasRole(['ADMIN', 'AUDITEUR']) ? [{ key: 'audit', label: 'Audit' }] : [])])
const canSeeSalary = computed(() => auth.hasRole(['RH', 'ADMIN', 'EMPLOYE']))
const canManageDocuments = computed(() => auth.hasRole(['RH', 'ADMIN']))
const canManageContracts = computed(() => auth.hasRole(['RH', 'ADMIN']))
const canManageEmployeeData = computed(() => auth.hasRole(['RH', 'ADMIN']))
const availablePosts = computed(() => posts.value.filter(post => !assignmentForm.departementId || Number(post.departementId) === Number(assignmentForm.departementId)))
const managerOptions = computed(() => employees.value.filter(employee => Number(employee.id) !== Number(route.params.id)))
function formatMoney(amount, currency = 'MGA') { return `${Number(amount || 0).toLocaleString('fr-FR', { maximumFractionDigits: 2 })} ${currency === 'MGA' ? 'Ar' : currency}` }
async function load() { try { const id = route.params.id; dossier.value = (await api.get(`/employes/${id}/dossier`)).data; career.value = (await api.get(`/employes/${id}/carriere`)).data; if (auth.hasRole(['ADMIN', 'AUDITEUR'])) auditRows.value = (await api.get('/audit', { params: { entite: 'Employe', utilisateurId: undefined } })).data.filter(row => row.entiteId === Number(id)) } catch (err) { error.value = formatApiError(err) } }
async function loadPosts() { try { posts.value = (await api.get('/postes', { params: { departementId: assignmentForm.departementId || undefined } })).data; if (!availablePosts.value.some(post => Number(post.id) === Number(assignmentForm.posteId))) assignmentForm.posteId = '' } catch (err) { error.value = formatApiError(err) } }
async function loadEmployeeChoices() { if (!canManageEmployeeData.value) return; try { const [departmentResponse, employeeResponse] = await Promise.all([api.get('/departements'), api.get('/employes')]); departments.value = departmentResponse.data; employees.value = employeeResponse.data; posts.value = (await api.get('/postes')).data } catch (err) { error.value = formatApiError(err) } }
async function saveAssignment() { saving.value = true; error.value = ''; try { await api.post(`/employes/${route.params.id}/affectations`, { ...assignmentForm, posteId: Number(assignmentForm.posteId), departementId: Number(assignmentForm.departementId), managerId: assignmentForm.managerId ? Number(assignmentForm.managerId) : null }); assignmentForm.dateAffectation = new Date().toISOString().slice(0, 10); await load() } catch (err) { error.value = formatApiError(err) } finally { saving.value = false } }
async function saveSalary() { saving.value = true; error.value = ''; try { await api.post(`/employes/${route.params.id}/salaires`, { ...salaryForm, montant: Number(salaryForm.montant), contratId: salaryForm.contratId ? Number(salaryForm.contratId) : null }); salaryForm.montant = ''; salaryForm.motif = ''; salaryForm.dateDebut = new Date().toISOString().slice(0, 10); salaryForm.contratId = ''; await load() } catch (err) { error.value = formatApiError(err) } finally { saving.value = false } }
async function download(doc) { try { const response = await api.get(`/onboarding-documents/${doc.id}`, { responseType: 'blob' }); const url = URL.createObjectURL(response.data); window.open(url, '_blank'); setTimeout(() => URL.revokeObjectURL(url), 30000) } catch (err) { error.value = formatApiError(err) } }
async function decide(doc, approve) { try { await api.post(`/documents/${doc.id}/${approve ? 'valider' : 'rejeter'}`, approve ? {} : { motif: window.prompt('Motif du rejet') || '' }); await load() } catch (err) { error.value = formatApiError(err) } }
async function signContract(contract) { const documentUrl = window.prompt('Lien du contrat signé (facultatif) :', contract.contratUrl || ''); if (documentUrl === null) return; contractBusy.value = true; error.value = ''; try { await api.post(`/contrats/${contract.id}/signer`, { documentUrl: documentUrl.trim() || null }); await load() } catch (err) { error.value = formatApiError(err) } finally { contractBusy.value = false } }
async function validateContract(contract) { if (!window.confirm('Confirmer la validation RH de ce contrat ?')) return; contractBusy.value = true; error.value = ''; try { await api.post(`/contrats/${contract.id}/valider`, {}); await load() } catch (err) { error.value = formatApiError(err) } finally { contractBusy.value = false } }
async function resetEmployeeAccess(contract) { if (!window.confirm('Générer un nouveau mot de passe temporaire pour ce salarié ? L’ancien mot de passe ne fonctionnera plus.')) return; contractBusy.value = true; error.value = ''; try { const { data } = await api.post(`/contrats/${contract.id}/reinitialiser-acces`); window.alert(`Identifiant : ${data.email}\nMot de passe temporaire (affiché une seule fois) : ${data.motDePasseTemporaire}`) } catch (err) { error.value = formatApiError(err) } finally { contractBusy.value = false } }
onMounted(() => { load(); loadEmployeeChoices() })
</script>
