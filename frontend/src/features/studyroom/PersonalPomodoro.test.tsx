import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { act, fireEvent, render, screen, within } from '@testing-library/react'
import PersonalPomodoro from './PersonalPomodoro'
import type { PanelVisibility } from './usePanelVisibility'

const panel = { hidden: false, hide: vi.fn(), show: vi.fn(), hideButton: { current: null } } as PanelVisibility
const fullscreen = { expanded: false, toggle: vi.fn() }
const pomodoro = (accountId: number) => <PersonalPomodoro accountId={accountId} panel={panel} fullscreen={fullscreen} />

beforeEach(() => { sessionStorage.clear(); vi.useFakeTimers(); vi.setSystemTime(new Date('2026-10-22T10:00:00Z')) })
afterEach(() => { vi.useRealTimers(); vi.restoreAllMocks(); sessionStorage.clear() })

function advance(milliseconds: number) { act(() => vi.advanceTimersByTime(milliseconds)) }

describe('personal Pomodoro', () => {
  it('starts, pauses and resumes without losing elapsed time', () => {
    render(pomodoro(1))
    expect(screen.getByRole('timer')).toHaveTextContent('25:00')
    fireEvent.click(screen.getByRole('button', { name: 'Start focus' }))
    advance(60_000)
    expect(screen.getByRole('timer')).toHaveTextContent('24:00')
    fireEvent.click(screen.getByRole('button', { name: 'Pause timer' }))
    advance(60_000)
    expect(screen.getByRole('timer')).toHaveTextContent('24:00')
    fireEvent.click(screen.getByRole('button', { name: 'Resume timer' }))
    advance(1_000)
    expect(screen.getByRole('timer')).toHaveTextContent('23:59')
  })

  it('recovers elapsed focus and break cycles after the browser was inactive', () => {
    render(pomodoro(1))
    fireEvent.click(screen.getByRole('button', { name: 'Start focus' }))
    act(() => vi.setSystemTime(new Date('2026-10-22T10:26:00Z')))
    fireEvent(window, new Event('focus'))
    expect(screen.getByRole('timer')).toHaveAttribute('aria-label', 'Break remaining 04:00')
    expect(screen.getByRole('status')).toHaveTextContent('Stretch, rest your eyes')
    expect(screen.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '20')
    act(() => vi.setSystemTime(new Date('2026-10-22T11:01:00Z')))
    fireEvent(document, new Event('visibilitychange'))
    expect(screen.getByRole('timer')).toHaveAttribute('aria-label', 'Focus remaining 24:00')
  })

  it('requires confirmation before ending a session and can cancel without losing progress', () => {
    render(pomodoro(1))
    fireEvent.click(screen.getByRole('button', { name: 'Start focus' }))
    advance(60_000)
    fireEvent.click(screen.getByRole('button', { name: 'End session' }))
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Cancel' }))
    expect(screen.getByRole('timer')).toHaveTextContent('24:00')
    fireEvent.click(screen.getByRole('button', { name: 'End session' }))
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'End session' }))
    expect(screen.getByRole('timer')).toHaveTextContent('25:00')
    expect(screen.getByRole('button', { name: 'Start focus' })).toBeInTheDocument()
  })

  it('validates duration edits before starting a fresh focus session', () => {
    render(pomodoro(1))
    fireEvent.click(screen.getByRole('button', { name: 'Settings' }))
    expect(screen.getByRole('tab', { name: 'Timer' })).toHaveAttribute('aria-selected', 'true')
    fireEvent.change(screen.getByLabelText('Focus minutes'), { target: { value: '0' } })
    fireEvent.change(screen.getByLabelText('Break minutes'), { target: { value: '2.5' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save timer settings' }))
    expect(screen.getAllByRole('alert')).toHaveLength(2)
    expect(screen.getByRole('timer')).toHaveTextContent('25:00')
    fireEvent.change(screen.getByLabelText('Focus minutes'), { target: { value: '40' } })
    fireEvent.change(screen.getByLabelText('Break minutes'), { target: { value: '10' } })
    fireEvent.click(screen.getByRole('button', { name: 'Save timer settings' }))
    expect(screen.getByText('Saved. Your fresh focus session is ready.')).toBeInTheDocument()
    expect(screen.getByRole('timer')).toHaveTextContent('40:00')
    fireEvent.click(screen.getByRole('button', { name: 'Close settings' }))
    fireEvent.click(screen.getByRole('button', { name: 'Start focus' }))
    act(() => vi.setSystemTime(new Date('2026-10-22T10:40:00Z')))
    fireEvent(window, new Event('focus'))
    expect(screen.getByRole('timer')).toHaveAttribute('aria-label', 'Break remaining 10:00')
  })

  it('restores a running timer in the same tab while keeping another account separate', () => {
    const view = render(pomodoro(1))
    fireEvent.click(screen.getByRole('button', { name: 'Start focus' }))
    advance(60_000)
    view.unmount()
    advance(60_000)
    const restored = render(pomodoro(1))
    expect(screen.getByRole('timer')).toHaveTextContent('23:00')
    expect(screen.getByRole('button', { name: 'Pause timer' })).toBeInTheDocument()
    restored.unmount()
    render(pomodoro(2))
    expect(screen.getByRole('timer')).toHaveTextContent('25:00')
    expect(screen.getByRole('button', { name: 'Start focus' })).toBeInTheDocument()
  })

  it('ignores corrupted stored durations and keeps working when storage is unavailable', () => {
    sessionStorage.setItem('study-buddy-pomodoro-1', JSON.stringify({ startedAt: 0, timer: { phase: 'FOCUS', status: 'RUNNING',
      remainingMillis: 1000, focusMillis: 0, breakMillis: -1 } }))
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new Error('Storage disabled') })
    render(pomodoro(1))
    expect(screen.getByRole('timer')).toHaveTextContent('25:00')
    fireEvent.click(screen.getByRole('button', { name: 'Start focus' }))
    advance(1_000)
    expect(screen.getByRole('timer')).toHaveTextContent('24:59')
  })
})
