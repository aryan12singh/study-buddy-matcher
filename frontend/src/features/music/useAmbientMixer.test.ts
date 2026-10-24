import { afterEach, describe, expect, it, vi } from 'vitest'
import { act, renderHook } from '@testing-library/react'
import { useAmbientMixer } from './useAmbientMixer'

describe('ambient master volume', () => {
  const created: HTMLAudioElement[] = []
  afterEach(() => { vi.restoreAllMocks(); created.length = 0 })

  function trackAudio() {
    const RealAudio = window.Audio
    // A constructor mock must be a regular function so `new Audio()` can call it.
    vi.spyOn(window, 'Audio').mockImplementation(function () {
      const audio = new RealAudio()
      created.push(audio)
      return audio
    })
  }

  it('scales every sound by the master volume and keeps each one\'s own level', () => {
    trackAudio()
    const { result } = renderHook(() => useAmbientMixer())
    act(() => result.current.setVolume('rain', 0.8))
    act(() => result.current.setVolume('cafe', 0.4))
    act(() => result.current.setMaster(0.5))
    expect(result.current.master).toBe(0.5)
    expect(created.map(audio => audio.volume)).toEqual([0.4, 0.2])
    // A sound's own slider still shows its level, and changing it respects the master.
    expect(result.current.channel('rain').volume).toBe(0.8)
    act(() => result.current.setVolume('rain', 1))
    expect(created[0].volume).toBe(0.5)
    act(() => result.current.setMaster(1))
    expect(created.map(audio => audio.volume)).toEqual([1, 0.4])
  })
})
