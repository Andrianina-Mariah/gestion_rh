export function formatApiError(error) {
  if (!error?.response) {
    return 'Le serveur backend est inaccessible. Démarrez PostgreSQL puis Spring Boot sur http://localhost:8080.'
  }
  const data = error?.response?.data
  if (typeof data === 'string') {
    return data
  }
  if (data?.message) {
    return data.message
  }
  if (data?.error) {
    return data.error
  }
  return 'Une erreur est survenue.'
}
