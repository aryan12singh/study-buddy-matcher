import { api } from '../../shared/api/client'

export type TimerPhase = 'FOCUS' | 'BREAK'
export type TimerStatus = 'IDLE' | 'RUNNING' | 'PAUSED'
export type PresenceState = 'PRESENT' | 'FOCUS' | 'BREAK'
export type RoomReactionType = 'CLAP' | 'HEART' | 'FIRE' | 'LIGHTBULB' | 'COFFEE' | 'SPROUT'
export type RoomReaction = { id: number; type: RoomReactionType; studentId: number; senderName: string; sentAt: string }
export type RoomTimer = { phase: TimerPhase; status: TimerStatus; remainingMillis: number; focusMillis: number; breakMillis: number }
export type StudyRoom = {
  groupId: number; groupName: string; version: number; serverTime: string
  focusMinutes: number; breakMinutes: number; maxFocusMinutes: number; maxBreakMinutes: number
  participantLimit: number; groupLimit: number; hostId: number; coHostId: number | null; hostOnline: boolean
  leader: boolean; canControl: boolean; joined: boolean; pollIntervalMillis: number; leaseLifetimeMillis: number
  timer: RoomTimer
  participants: { studentId: number; name: string; presence: PresenceState; expiresAt: string; leader: boolean; host: boolean; coHost: boolean }[]
  members: { studentId: number; name: string }[]
  /** Reactions sent in the last few seconds, oldest first. */
  reactions?: RoomReaction[]
}
export type RoomSnapshot = { room: StudyRoom; receivedAt: number; estimatedLatency: number }
export type RoomConfiguration = { focusMinutes: number; breakMinutes: number; participantLimit: number; hostId: number | null; coHostId: number | null; expectedVersion: number }

async function snapshot(request: () => Promise<{ data: StudyRoom }>): Promise<RoomSnapshot> {
  const started = performance.now()
  const response = await request()
  const receivedAt = performance.now()
  return { room: response.data, receivedAt, estimatedLatency: (receivedAt - started) / 2 }
}
const path = (groupId: number) => `/groups/${groupId}/room`
export function getRoom(groupId: number, signal?: AbortSignal) { return snapshot(() => api.get(path(groupId), { signal })) }
export function joinRoom(groupId: number, clientId: string) { return snapshot(() => api.post(`${path(groupId)}/join`, { clientId })) }
export function heartbeatRoom(groupId: number, clientId: string, presence: PresenceState, signal?: AbortSignal) {
  return snapshot(() => api.put(`${path(groupId)}/presence`, { clientId, presence }, { signal }))
}
export async function leaveRoom(groupId: number, clientId: string) { await api.delete(`${path(groupId)}/presence/${clientId}`) }
/** Leaves as the tab closes or reloads; a keepalive fetch can outlive the page, unlike an ordinary request. */
export async function leaveRoomOnExit(groupId: number, clientId: string) {
  await api.delete(`${path(groupId)}/presence/${clientId}`, { adapter: 'fetch', fetchOptions: { keepalive: true } })
}
export function timerCommand(groupId: number, command: 'START' | 'PAUSE' | 'RESUME' | 'RESET', expectedVersion: number) {
  return snapshot(() => api.post(`${path(groupId)}/timer`, { command, expectedVersion }))
}
export function configureRoom(groupId: number, configuration: RoomConfiguration) {
  return snapshot(() => api.put(`${path(groupId)}/settings`, configuration))
}
export async function sendReaction(groupId: number, type: RoomReactionType) {
  await api.post(`${path(groupId)}/reactions`, { type })
}
