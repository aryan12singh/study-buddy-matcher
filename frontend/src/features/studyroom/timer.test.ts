import { describe, expect, it } from 'vitest'
import { projectTimer, timerText } from './timer'
import type { RoomTimer } from './api'

const running: RoomTimer = { phase: 'FOCUS', status: 'RUNNING', remainingMillis: 5000, focusMillis: 60000, breakMillis: 10000 }
describe('timer rendering from server snapshots', () => {
  it('counts down without advancing paused or idle timers', () => {
    expect(projectTimer(running, 1400)).toEqual({ phase: 'FOCUS', remainingMillis: 3600 })
    expect(projectTimer({ ...running, status: 'PAUSED' }, 100000)).toEqual({ phase: 'FOCUS', remainingMillis: 5000 })
    expect(projectTimer({ ...running, status: 'IDLE' }, 100000)).toEqual({ phase: 'FOCUS', remainingMillis: 5000 })
  })
  it('crosses focus/break boundaries and catches up after background suspension', () => {
    expect(projectTimer(running, 5000)).toEqual({ phase: 'BREAK', remainingMillis: 10000 })
    expect(projectTimer(running, 15000)).toEqual({ phase: 'FOCUS', remainingMillis: 60000 })
    expect(projectTimer(running, 75_000 + 7 * 70_000)).toEqual({ phase: 'BREAK', remainingMillis: 10000 })
  })
  it('handles a running break, fractional milliseconds and backward local time', () => {
    expect(projectTimer({ ...running, phase: 'BREAK' }, 5000)).toEqual({ phase: 'FOCUS', remainingMillis: 60000 })
    expect(projectTimer(running, 4999.75).remainingMillis).toBe(0.25)
    expect(projectTimer(running, -50).remainingMillis).toBe(5000)
  })
  it('rounds displayed seconds up without showing negative time', () => {
    expect(timerText(1)).toBe('00:01')
    expect(timerText(60_001)).toBe('01:01')
    expect(timerText(-1)).toBe('00:00')
  })
})
