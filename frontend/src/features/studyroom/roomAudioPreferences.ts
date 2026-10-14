export type RoomAudioPreferences = {
  volume: { music: number; ambient: number }
  muted: { music: boolean; ambient: boolean }
}

const STORAGE_KEY = 'study-room-audio-preferences'

function record(value: unknown): Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value) ? value as Record<string, unknown> : {}
}

function volume(value: unknown) {
  return typeof value === 'number' && Number.isFinite(value) ? Math.round(Math.min(100, Math.max(0, value))) : 25
}

/** Only nonsensitive local gain/mute choices are remembered; playback consent is never stored. */
export function readRoomAudioPreferences(): RoomAudioPreferences {
  let saved: Record<string, unknown> = {}
  try { saved = record(JSON.parse(localStorage.getItem(STORAGE_KEY) ?? 'null')) } catch { /* Storage is optional. */ }
  const savedVolume = record(saved.volume)
  const savedMuted = record(saved.muted)
  return {
    volume: { music: volume(savedVolume.music), ambient: volume(savedVolume.ambient) },
    muted: { music: savedMuted.music === true, ambient: savedMuted.ambient === true },
  }
}

export function saveRoomAudioPreferences(preferences: RoomAudioPreferences) {
  try { localStorage.setItem(STORAGE_KEY, JSON.stringify(preferences)) } catch { /* The mixer still works without storage. */ }
}
