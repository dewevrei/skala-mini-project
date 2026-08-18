import { apiEnvelopeClient } from './client'

export const taskApi = {
  create: (projectId, columnId, payload) => apiEnvelopeClient.post(
    `/projects/${projectId}/columns/${columnId}/tasks`,
    payload,
  ),
  generate: (projectId, payload) => apiEnvelopeClient.post(
    `/projects/${projectId}/tasks/ai-generate`,
    payload,
  ),
  get: (projectId, taskId) => apiEnvelopeClient.get(`/projects/${projectId}/tasks/${taskId}`),
  update: (projectId, taskId, payload) => apiEnvelopeClient.patch(
    `/projects/${projectId}/tasks/${taskId}`,
    payload,
  ),
  remove: (projectId, taskId) => apiEnvelopeClient.delete(`/projects/${projectId}/tasks/${taskId}`),
  updateDates: (projectId, taskId, payload) => apiEnvelopeClient.patch(
    `/projects/${projectId}/tasks/${taskId}/dates`,
    payload,
  ),
  changeStatus: (projectId, taskId, targetColumnId) => apiEnvelopeClient.patch(
    `/projects/${projectId}/tasks/${taskId}/status`,
    { targetColumnId },
  ),
  move: (projectId, taskId, payload) => apiEnvelopeClient.patch(
    `/projects/${projectId}/tasks/${taskId}/position`,
    payload,
  ),
}
