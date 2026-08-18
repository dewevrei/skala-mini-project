import { apiEnvelopeClient } from './client'

export const projectApi = {
  list: () => apiEnvelopeClient.get('/projects'),
  get: (projectId) => apiEnvelopeClient.get(`/projects/${projectId}`),
  create: (payload) => apiEnvelopeClient.post('/projects', payload),
  update: (projectId, payload) => apiEnvelopeClient.patch(`/projects/${projectId}`, payload),
  remove: (projectId) => apiEnvelopeClient.delete(`/projects/${projectId}`),
  board: (projectId) => apiEnvelopeClient.get(`/projects/${projectId}/board`),
  items: (projectId, title = '') => apiEnvelopeClient.get(`/projects/${projectId}/items`, {
    params: title.trim() ? { title: title.trim() } : undefined,
  }),
}
