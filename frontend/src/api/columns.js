import { apiEnvelopeClient } from './client'

export const columnApi = {
  create: (projectId, payload) => apiEnvelopeClient.post(`/projects/${projectId}/columns`, payload),
  update: (projectId, columnId, payload) => apiEnvelopeClient.patch(
    `/projects/${projectId}/columns/${columnId}`,
    payload,
  ),
  reorder: (projectId, orderedColumnIds) => apiEnvelopeClient.put(
    `/projects/${projectId}/columns/order`,
    { orderedColumnIds },
  ),
  remove: (projectId, columnId) => apiEnvelopeClient.delete(
    `/projects/${projectId}/columns/${columnId}`,
  ),
}
