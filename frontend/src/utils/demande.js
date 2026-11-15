export const DEMANDE_STATUTS = [
  'BROUILLON',
  'EN_ATTENTE_RH',
  'ANALYSE_FINANCE',
  'EN_ATTENTE_DIRECTION',
  'A_CORRIGER',
  'VALIDE',
  'REJETE',
  'RECRUTEMENT_OUVERT'
]

export const DEMANDE_TYPES = ['CREATION', 'REMPLACEMENT']

export function statutLabel(value) {
  const labels = {
    BROUILLON: 'Brouillon',
    EN_ATTENTE_RH: 'En attente RH',
    ANALYSE_FINANCE: 'Analyse finance',
    EN_ATTENTE_DIRECTION: 'En attente Direction',
    A_CORRIGER: 'À corriger',
    VALIDE: 'Validée',
    REJETE: 'Rejetée',
    RECRUTEMENT_OUVERT: 'Recrutement ouvert'
  }
  return labels[value] || value
}

export function typeLabel(value) {
  return value === 'CREATION' ? 'Création' : 'Remplacement'
}

export function validateDemande(form) {
  const errors = {}
  const nombre = Number(form.nombrePostes)
  const budget = Number(form.budget)
  const dateValue = form.dateSouhaitee ? new Date(form.dateSouhaitee) : null
  const today = new Date()
  today.setHours(0, 0, 0, 0)

  if (!form.posteId) errors.posteId = 'Le poste est obligatoire.'
  if (!form.type) errors.type = 'Le type est obligatoire.'
  if (!Number.isInteger(nombre) || nombre < 1) {
    errors.nombrePostes = 'Le nombre de postes doit être au moins 1.'
  }
  if (!form.motif?.trim()) errors.motif = 'Le motif est obligatoire.'
  if (!Number.isFinite(budget) || budget <= 0) {
    errors.budget = 'Le budget doit être supérieur à 0.'
  }
  if (!dateValue) {
    errors.dateSouhaitee = 'La date souhaitée est obligatoire.'
  } else if (dateValue <= today) {
    errors.dateSouhaitee = "La date souhaitée doit être postérieure à aujourd'hui."
  }

  return errors
}
