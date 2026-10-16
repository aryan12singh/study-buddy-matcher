import { afterEach, describe, expect, it, vi } from 'vitest'
import { readRoomAudioPreferences, saveRoomAudioPreferences } from './roomAudioPreferences'

afterEach(() => { vi.restoreAllMocks(); localStorage.clear() })

describe('optional browser audio preferences', () => {
  it('clamps finite gains and rejects malformed or wrongly typed values independently', () => {
    localStorage.setItem('study-room-audio-preferences', JSON.stringify({ volume: { music: 300, ambient: -7 }, muted: { music: true, ambient: 'true' }, enabled: true }))
    expect(readRoomAudioPreferences()).toEqual({ volume: { music: 100, ambient: 0 }, muted: { music: true, ambient: false } })
    localStorage.setItem('study-room-audio-preferences', JSON.stringify({ volume: { music: '42', ambient: null }, muted: [] }))
    expect(readRoomAudioPreferences()).toEqual({ volume: { music: 25, ambient: 25 }, muted: { music: false, ambient: false } })
    localStorage.setItem('study-room-audio-preferences', '{broken')
    expect(readRoomAudioPreferences()).toEqual({ volume: { music: 25, ambient: 25 }, muted: { music: false, ambient: false } })
  })

  it('keeps safe defaults and usable preferences when storage is unavailable', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => { throw new Error('Storage blocked') })
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new Error('Storage full') })
    expect(readRoomAudioPreferences()).toEqual({ volume: { music: 25, ambient: 25 }, muted: { music: false, ambient: false } })
    expect(() => saveRoomAudioPreferences({ volume: { music: 40, ambient: 60 }, muted: { music: false, ambient: true } })).not.toThrow()
  })
})
