import { api } from '../../shared/api/client'
import type { WeeklySlot } from '../../shared/api/types'

export type SessionScope = { kind: 'GROUP' | 'BUDDY'; id: number }
export type SessionOccurrence = { startAt: string; endAt: string }
export type StudySession = {
  id: number; version: number; scope: 'GROUP' | 'BUDDY'; groupId: number | null; connectionId: number | null
  title: string; startLocal: string; endLocal: string; startAt: string; endAt: string; timeZone: string
  occurrenceCount: number; agenda: string | null; location: string | null; roomLink: string | null
  status: 'SCHEDULED' | 'CANCELLED'; canManage: boolean; occurrences: SessionOccurrence[]
  nextOccurrence: SessionOccurrence | null; updatedAt: string; cancelledAt: string | null
}
export type SessionInput = { title: string; startLocal: string; endLocal: string; timeZone: string; occurrenceCount: number; agenda: string | null; location: string | null; roomLink: string | null }
export type SessionAvailability = { timeZone: string; participantCount: number; sharedSlots: WeeklySlot[]; sharedMinutes: number }
const scopePath = (scope: SessionScope) => `/${scope.kind === 'GROUP' ? 'groups' : 'connections'}/${scope.id}/sessions`
export const scopeListPath = scopePath
export async function getMySessions(signal?: AbortSignal) { return (await api.get<StudySession[]>('/sessions/mine', { signal })).data }
export async function getSessions(scope: SessionScope, signal?: AbortSignal) { return (await api.get<StudySession[]>(scopePath(scope), { signal })).data }
export async function getSession(id: number, signal?: AbortSignal) { return (await api.get<StudySession>(`/sessions/${id}`, { signal })).data }
export async function getAvailability(scope: SessionScope, signal?: AbortSignal) { return (await api.get<SessionAvailability>(`${scopePath(scope)}/availability`, { signal })).data }
export async function createSession(scope: SessionScope, input: SessionInput) { return (await api.post<StudySession>(scopePath(scope), input)).data }
export async function updateSession(id: number, input: SessionInput, expectedVersion: number) { return (await api.put<StudySession>(`/sessions/${id}`, { ...input, expectedVersion })).data }
export async function cancelSession(id: number, expectedVersion: number) { return (await api.post<StudySession>(`/sessions/${id}/cancel`, { expectedVersion })).data }
export async function getCalendar(id: number, signal?: AbortSignal) {
  return (await api.get<Blob>(`/sessions/${id}/calendar`, { signal, responseType: 'blob', headers: { Accept: 'text/calendar' } })).data
}
