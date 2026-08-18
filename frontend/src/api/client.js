import axios from 'axios'

const API_ORIGIN = 'http://localhost:8080'
const STATE_CHANGING = new Set(['post', 'put', 'patch', 'delete'])

export class ApiError extends Error {
  constructor(response, status) {
    super(response?.message ?? '요청을 처리하지 못했습니다.')
    this.name = 'ApiError'
    this.code = response?.code ?? 'NETWORK_ERROR'
    this.status = status
    this.data = response?.data ?? null
  }
}

const csrfClient = axios.create({
  baseURL: `${API_ORIGIN}/api/v1`,
  withCredentials: true,
})

export const apiClient = axios.create({
  baseURL: `${API_ORIGIN}/api/v1`,
  withCredentials: true,
  headers: { Accept: 'application/json' },
})

let csrf = null
let csrfRequest = null
let authenticationFailureHandler = null

export function onAuthenticationFailure(handler) {
  authenticationFailureHandler = handler
}

export async function ensureCsrfToken() {
  if (csrf) return csrf
  if (!csrfRequest) {
    csrfRequest = csrfClient.get('/auth/csrf')
      .then(({ data }) => {
        if (!data?.success || !data.data?.token || !data.data?.headerName) {
          throw new ApiError(data)
        }
        csrf = data.data
        return csrf
      })
      .catch((error) => {
        if (error instanceof ApiError) throw error
        throw new ApiError(error.response?.data, error.response?.status)
      })
      .finally(() => { csrfRequest = null })
  }
  return csrfRequest
}

export function clearCsrfToken() {
  csrf = null
}

apiClient.interceptors.request.use(async (config) => {
  if (STATE_CHANGING.has(config.method?.toLowerCase())) {
    const token = await ensureCsrfToken()
    config.headers[token.headerName] = token.token
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => {
    const envelope = response.data
    if (!envelope?.success) throw new ApiError(envelope, response.status)
    return envelope.data
  },
  (error) => {
    const apiError = new ApiError(error.response?.data, error.response?.status)
    if (apiError.code === 'CSRF_TOKEN_INVALID') clearCsrfToken()
    if (apiError.code === 'AUTHENTICATION_REQUIRED') authenticationFailureHandler?.(apiError)
    return Promise.reject(apiError)
  },
)

export const backendLoginUrl = `${API_ORIGIN}/oauth2/authorization/google`
