import { api } from '../../shared/api/client'

export type TimerPhase = 'FOCUS' | 'BREAK'
export type TimerStatus = 'IDLE' | 'RUNNING' | 'PAUSED'
export type PresenceState = 'PRESENT' | 'FOCUS' | 'BREAK'
export type AudioPreset = 'CALM_MUSIC' | 'RAIN' | 'WHITE_NOISE' | 'CAFE'
export type RoomTimer = { phase: TimerPhase; status: TimerStatus; remainingMillis: number; focusMillis: number; breakMillis: number }
export type StudyRoom = {
  groupId: number; groupName: string; version: number; serverTime: string
  focusMinutes: number; breakMinutes: number; maxFocusMinutes: number; maxBreakMinutes: number
  participantLimit: number; groupLimit: number; hostId: number; coHostId: number | null; hostOnline: boolean
  leader: boolean; canControl: boolean; joined: boolean; pollIntervalMillis: number; leaseLifetimeMillis: number
  timer: RoomTimer; audio: { preset: AudioPreset; playing: boolean }
  audioPresets: { id: AudioPreset; label: string; kind: string }[]
  participants: { studentId: number; name: string; presence: PresenceState; expiresAt: string; leader: boolean; host: boolean; coHost: boolean }[]
  members: { studentId: number; name: string }[]
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
export function timerCommand(groupId: number, command: 'START' | 'PAUSE' | 'RESUME' | 'RESET', expectedVersion: number) {
  return snapshot(() => api.post(`${path(groupId)}/timer`, { command, expectedVersion }))
}
export function selectAudio(groupId: number, preset: AudioPreset, playing: boolean, expectedVersion: number) {
  return snapshot(() => api.put(`${path(groupId)}/audio`, { preset, playing, expectedVersion }))
}
export function configureRoom(groupId: number, configuration: RoomConfiguration) {
  return snapshot(() => api.put(`${path(groupId)}/settings`, configuration))
}
