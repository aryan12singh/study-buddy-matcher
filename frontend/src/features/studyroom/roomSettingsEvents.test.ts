import { describe, expect, it, vi } from 'vitest'
import { onOpenRoomSettings, openRoomSettings } from './roomSettingsEvents'

describe('room settings requests', () => {
  it('delivers a request to the menu on the page straight away', () => {
    const listener = vi.fn()
    const stop = onOpenRoomSettings(listener)
    openRoomSettings('music')
    expect(listener).toHaveBeenCalledExactlyOnceWith('music')
    stop()
  })

  it('keeps a request made before any menu appears for the first menu, once', () => {
    openRoomSettings('music')
    const first = vi.fn(), second = vi.fn()
    const stopFirst = onOpenRoomSettings(first)
    const stopSecond = onOpenRoomSettings(second)
    expect(first).toHaveBeenCalledExactlyOnceWith('music')
    expect(second).not.toHaveBeenCalled()
    stopFirst()
    stopSecond()
  })
})
