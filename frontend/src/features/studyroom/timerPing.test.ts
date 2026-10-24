import { afterAll, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { renderHook } from '@testing-library/react'
import type { TimerPhase, TimerStatus } from './api'
import { useTimerPings } from './timerPing'

// A stand-in audio device that records every note played. A chime starting on 660 Hz is a start.
const played: number[] = []
class FakeAudioContext {
  state = 'running'
  currentTime = 0
  destination = {}
  createGain() { return { gain: { setValueAtTime() {}, exponentialRampToValueAtTime() {} }, connect: (next: unknown) => next } }
  createOscillator() {
    const tone = { type: '', frequency: { value: 0 }, connect: (next: unknown) => next, stop() {},
      start: () => { played.push(tone.frequency.value) } }
    return tone
  }
}

function chimes() {
  // Each chime is two notes; keep the first of each pair.
  return played.filter((_, index) => index % 2 === 0).map(note => note === 660 ? 'start' : 'end')
}

describe('timer chimes', () => {
  beforeAll(() => { vi.stubGlobal('AudioContext', FakeAudioContext) })
  afterAll(() => { vi.unstubAllGlobals() })
  beforeEach(() => { played.length = 0 })

  function track(initial: { status: TimerStatus; phase?: TimerPhase; enabled?: boolean }) {
    return renderHook(props => useTimerPings(props.status, props.phase, props.enabled ?? true), { initialProps: initial })
  }

  it('stays quiet for the first state, then chimes when a session starts and ends', () => {
    const { rerender } = track({ status: 'RUNNING', phase: 'FOCUS' })
    expect(chimes()).toEqual([])
    rerender({ status: 'IDLE', phase: 'FOCUS' })
    rerender({ status: 'RUNNING', phase: 'FOCUS' })
    expect(chimes()).toEqual(['end', 'start'])
  })

  it('chimes when focus gives way to a break and back, but not on pause and resume', () => {
    const { rerender } = track({ status: 'RUNNING', phase: 'FOCUS' })
    rerender({ status: 'PAUSED', phase: 'FOCUS' })
    rerender({ status: 'RUNNING', phase: 'FOCUS' })
    rerender({ status: 'RUNNING', phase: 'BREAK' })
    rerender({ status: 'RUNNING', phase: 'FOCUS' })
    expect(chimes()).toEqual(['end', 'start'])
  })

  it('stays quiet when switched off, as for someone only previewing a room', () => {
    const { rerender } = track({ status: 'IDLE', enabled: false })
    rerender({ status: 'RUNNING', enabled: false })
    expect(chimes()).toEqual([])
  })
})
