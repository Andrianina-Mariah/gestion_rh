<template>
  <section class="page">
    <div class="page-heading"><div><h2>Mon dossier</h2><p class="muted">Données personnelles autorisées et documents déposés.</p></div></div>
    <div v-if="error" class="alert error">{{ error }}</div><div v-if="message" class="alert success">{{ message }}</div>
    <section v-if="dossier" class="card detail-grid"><div><strong>Matricule :</strong> {{ dossier.identite.matricule }}</div><div><strong>Statut :</strong> {{ dossier.identite.statut }}</div><div><strong>Nom :</strong> {{ dossier.identite.prenom }} {{ dossier.identite.nom }}</div><div><strong>Email :</strong> {{ dossier.identite.email }}</div><div><strong>Téléphone :</strong> {{ dossier.identite.telephone || '—' }}</div><div><strong>Ancienneté :</strong> {{ dossier.anciennete }}</div></section>
    <form class="card form-grid" @submit.prevent="requestChange"><h3 class="full">Demander une modification</h3><label>Champ <select v-model="form.champ"><option value="telephone">Téléphone</option><option value="adresse">Adresse</option><option value="nationalite">Nationalité</option><option value="situationFamiliale">Situation familiale</option></select></label><label>Nouvelle valeur <input v-model="form.nouvelleValeur" required></label><label class="full">Commentaire <textarea v-model="form.commentaire"></textarea></label><div class="actions"><button class="button primary">Soumettre à la RH</button></div></form>
    <section v-if="dossier" class="card"><h3>Documents</h3><form class="toolbar" @submit.prevent="upload"><label>Type <select v-model="uploadForm.type"><option>CIN</option><option>CV</option><option>RIB</option><option>DIPLOME</option></select></label><label>Fichier PDF <input type="file" accept="application/pdf" @change="uploadForm.file = $event.target.files[0]" required></label><button class="button primary">Déposer</button></form><div v-for="doc in dossier.documents" :key="doc.id" class="evaluation-row"><strong>{{ doc.type }} v{{ doc.version }}</strong><span>{{ doc.statut }} · {{ doc.dateDepot?.slice(0, 10) }}</span></div></section>
  </section>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue'
import api from '@/api/axios'
import { formatApiError } from '@/utils/errors'
const dossier = ref(null); const error = ref(''); const message = ref(''); const form = reactive({ champ: 'telephone', nouvelleValeur: '', commentaire: '' }); const uploadForm = reactive({ type: 'CIN', file: null })
async function load() { try { dossier.value = (await api.get('/moi/dossier')).data } catch (err) { error.value = formatApiError(err) } }
async function requestChange() { try { await api.put('/moi/dossier', { ...form, ancienneValeur: dossier.value?.identite?.[form.champ] || '' }); message.value = 'Demande envoyée à la RH.'; form.nouvelleValeur = ''; form.commentaire = '' } catch (err) { error.value = formatApiError(err) } }
async function upload() { try { const data = new FormData(); data.append('type', uploadForm.type); data.append('file', uploadForm.file); await api.post(`/employes/${dossier.value.identite.id}/documents`, data, { headers: { 'Content-Type': 'multipart/form-data' } }); message.value = 'Document déposé.'; await load() } catch (err) { error.value = formatApiError(err) } }
onMounted(load)
</script>
