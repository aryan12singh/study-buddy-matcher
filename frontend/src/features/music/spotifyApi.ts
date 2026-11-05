import { api } from '../../shared/api/client'

export type SpotifyStatus = {
  configured: boolean
  connected: boolean
  embedPlaylistId: string
}

export type SpotifyAccessToken = {
  accessToken: string
  expiresAt: string
}

export async function getSpotifyStatus(signal?: AbortSignal) {
  return (await api.get<SpotifyStatus>('/spotify/status', { signal })).data
}

/** Returns the Spotify sign-in page to open; Spotify then sends the browser back to the Study Room. */
export async function startSpotifyAuthorization() {
  return (await api.post<{ url: string }>('/spotify/authorization')).data.url
}

/**
 * Finishes a sign-in as the signed-in student. The server connects Spotify only if this student
 * started it, so a sign-in link someone else sent can never hand them this student's account.
 */
export async function completeSpotifyAuthorization(state: string, code: string) {
  return (await api.post<{ outcome: string }>('/spotify/authorization/complete', { state, code })).data.outcome
}

export async function getSpotifyAccessToken() {
  return (await api.get<SpotifyAccessToken>('/spotify/token')).data
}
