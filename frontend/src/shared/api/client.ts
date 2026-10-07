import axios from 'axios'

const configuredBase = import.meta.env.VITE_API_BASE_URL?.trim().replace(/\/$/, '')

/** The configured URL may be the server origin or its /api prefix. */
export const api = axios.create({
  baseURL: configuredBase ? (configuredBase.endsWith('/api') ? configuredBase : `${configuredBase}/api`) : '/api',
  timeout: 15000,
  headers: { Accept: 'application/json' },
})

export const SESSION_TOKEN_KEY = 'study-buddy-session'
let accessToken: string | null = sessionStorage.getItem(SESSION_TOKEN_KEY)

export function setAccessToken(token: string | null) {
  accessToken = token
  if (token) sessionStorage.setItem(SESSION_TOKEN_KEY, token)
  else sessionStorage.removeItem(SESSION_TOKEN_KEY)
}

export function getAccessToken() { return accessToken }

api.interceptors.request.use((request) => {
  if (accessToken && !request.url?.startsWith('/auth/login') && !request.url?.startsWith('/auth/register')) {
    request.headers.Authorization = `Bearer ${accessToken}`
  }
  return request
})

api.interceptors.response.use((response) => response, (error: unknown) => {
  if (axios.isAxiosError(error) && error.response?.status === 401 && !error.config?.url?.startsWith('/auth/login')
    && error.config?.headers?.Authorization === `Bearer ${accessToken}`) {
    setAccessToken(null)
    window.dispatchEvent(new Event('session-expired'))
    refreshResources(true)
  }
  return Promise.reject(error)
})

export type Problem = { message?: string; detail?: string; title?: string; code?: string; fieldErrors?: Record<string, string> }

export function errorMessage(error: unknown): string {
  if (axios.isAxiosError<Problem>(error)) {
    if (!error.response) return 'Unable to reach the server. Check your connection and try again.'
    return error.response.data?.message || error.response.data?.detail || error.response.data?.title || 'This action could not be completed. Try again.'
  }
  return error instanceof Error ? error.message : 'This action could not be completed. Try again.'
}

export function fieldErrors(error: unknown): Record<string, string> {
  return axios.isAxiosError<Problem>(error) ? error.response?.data?.fieldErrors || {} : {}
}

/** Purge before refetching when contact permissions or the signed-in account change. */
export function refreshResources(purge = false) {
  window.dispatchEvent(new CustomEvent('resources-changed', { detail: { purge } }))
}
