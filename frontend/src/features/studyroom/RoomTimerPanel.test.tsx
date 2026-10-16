import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { act, fireEvent, render, screen } from '@testing-library/react'
import type { RoomSnapshot, RoomTimer, StudyRoom } from './api'
import RoomTimerPanel from './RoomTimerPanel'

let now = 0
const startTime = Date.parse('2026-10-14T10:00:00Z')
const running: RoomTimer = { phase: 'FOCUS', status: 'RUNNING', remainingMillis: 500, focusMillis: 1000, breakMillis: 500 }
function snapshot(timer: RoomTimer, at = now): RoomSnapshot {
  return { receivedAt: at, estimatedLatency: 0, room: { groupId: 5, serverTime: new Date(startTime + at).toISOString(),
    timer, focusMinutes: 1, breakMinutes: 1 } as StudyRoom }
}
function panel(value: RoomSnapshot) { return <RoomTimerPanel snapshot={value} canControl={false} pending={false} onControl={vi.fn()} /> }
function tick() { act(() => { now += 250; vi.advanceTimersByTime(250) }) }
beforeEach(() => { now = 0; vi.useFakeTimers(); vi.spyOn(performance, 'now').mockImplementation(() => now) })
afterEach(() => { vi.restoreAllMocks(); vi.useRealTimers() })

describe('room timer transition cue', () => {
  it('announces each observed phase boundary once, without repeating on countdown ticks or consistent snapshots', () => {
    const view = render(panel(snapshot(running)))
    const cue = screen.getByRole('status')
    expect(cue).toBeEmptyDOMElement()
    expect(cue).toHaveAttribute('aria-live', 'polite')
    expect(cue).toHaveAttribute('aria-atomic', 'true')
    tick()
    expect(cue).toBeEmptyDOMElement()
    tick()
    expect(cue).toHaveTextContent('Break started. Take a moment to recharge.')
    const message = cue.firstChild
    tick()
    view.rerender(panel(snapshot({ ...running, phase: 'BREAK', remainingMillis: 250 })))
    expect(cue.firstChild).toBe(message)
    tick()
    expect(cue).toHaveTextContent('Focus started. Time to study together.')
    const focusMessage = cue.firstChild
    tick()
    expect(cue.firstChild).toBe(focusMessage)
  })

  it('does not announce initial joins, reset/start snapshots, pause/resume or a changed timer epoch as historic transitions', () => {
    const view = render(panel(snapshot({ ...running, phase: 'BREAK', remainingMillis: 500 })))
    const cue = screen.getByRole('status')
    expect(cue).toBeEmptyDOMElement()
    tick()
    view.rerender(panel(snapshot({ ...running, phase: 'FOCUS', status: 'IDLE', remainingMillis: 1000 })))
    expect(cue).toBeEmptyDOMElement()
    view.rerender(panel(snapshot({ ...running, phase: 'FOCUS', remainingMillis: 1000 })))
    tick()
    view.rerender(panel(snapshot({ ...running, phase: 'BREAK', remainingMillis: 500 })))
    expect(cue).toBeEmptyDOMElement()
    tick()
    view.rerender(panel(snapshot({ ...running, phase: 'BREAK', status: 'PAUSED', remainingMillis: 250 })))
    expect(cue).toBeEmptyDOMElement()
    view.rerender(panel(snapshot({ ...running, phase: 'BREAK', remainingMillis: 250 })))
    expect(cue).toBeEmptyDOMElement()
    tick()
    expect(cue).toHaveTextContent('Focus started.')
    view.unmount()
    render(panel(snapshot({ ...running, phase: 'BREAK', remainingMillis: 500 })))
    expect(screen.getByRole('status')).toBeEmptyDOMElement()
  })

  it('does not reverse or repeat an observed phase cue when a refreshed latency estimate moves across its boundary', () => {
    const view = render(panel({ ...snapshot({ ...running, remainingMillis: 600 }), estimatedLatency: 200 }))
    const cue = screen.getByRole('status')
    tick()
    tick()
    expect(cue).toHaveTextContent('Break started.')
    const message = cue.firstChild
    view.rerender(panel(snapshot({ ...running, remainingMillis: 100 })))
    expect(cue.firstChild).toBe(message)
    tick()
    expect(cue.firstChild).toBe(message)
    tick()
    tick()
    expect(cue).toHaveTextContent('Focus started.')
  })

  it('rebases after returning to the browser or a missed observation instead of announcing elapsed phases', () => {
    render(panel(snapshot(running)))
    const cue = screen.getByRole('status')
    tick()
    fireEvent(window, new Event('focus'))
    tick()
    expect(cue).toBeEmptyDOMElement()
    tick()
    tick()
    expect(cue).toHaveTextContent('Focus started.')
    act(() => { now += 4250; vi.advanceTimersByTime(250) })
    expect(cue).toBeEmptyDOMElement()
  })
})
