import { useEffect, useState } from 'react'
import type { RoomTimer } from './api'
import { projectTimer } from './timer'

export const POMODORO_DEFAULTS = { focusMinutes: 25, breakMinutes: 5 }
export const POMODORO_LIMITS = { focusMinutes: 180, breakMinutes: 60 }
export const MINUTE_MILLIS = 60_000
const TICK_MILLIS = 250

type PersonalPomodoroState = { timer: RoomTimer; startedAt: number }

function initialState(focusMinutes = POMODORO_DEFAULTS.focusMinutes, breakMinutes = POMODORO_DEFAULTS.breakMinutes): PersonalPomodoroState {
  return { startedAt: 0, timer: { phase: 'FOCUS', status: 'IDLE', remainingMillis: focusMinutes * MINUTE_MILLIS,
    focusMillis: focusMinutes * MINUTE_MILLIS, breakMillis: breakMinutes * MINUTE_MILLIS } }
}

function readState(key: string): PersonalPomodoroState {
  try {
    const state = JSON.parse(sessionStorage.getItem(key) || 'null') as PersonalPomodoroState | null
    if (!state) return initialState()
    const timer = state.timer
    if (timer && ['FOCUS', 'BREAK'].includes(timer.phase) && ['IDLE', 'RUNNING', 'PAUSED'].includes(timer.status)
      && Number.isFinite(state.startedAt) && state.startedAt >= 0
      && Number.isFinite(timer.focusMillis) && Number.isFinite(timer.breakMillis)
      && Number.isInteger(timer.focusMillis / MINUTE_MILLIS) && timer.focusMillis >= MINUTE_MILLIS
      && timer.focusMillis <= POMODORO_LIMITS.focusMinutes * MINUTE_MILLIS
      && Number.isInteger(timer.breakMillis / MINUTE_MILLIS) && timer.breakMillis >= MINUTE_MILLIS
      && timer.breakMillis <= POMODORO_LIMITS.breakMinutes * MINUTE_MILLIS
      && Number.isFinite(timer.remainingMillis) && timer.remainingMillis > 0
      && timer.remainingMillis <= (timer.phase === 'FOCUS' ? timer.focusMillis : timer.breakMillis)) return state
  } catch { /* A timer still works when browser storage is unavailable. */ }
  return initialState()
}

/** A personal timer belongs to this account and browser tab; group timers remain server-owned. */
export function usePersonalPomodoro(accountId: number) {
  const storageKey = `study-buddy-pomodoro-${accountId}`
  const [state, setState] = useState(() => readState(storageKey))
  const [now, setNow] = useState(Date.now)
  useEffect(() => {
    try { sessionStorage.setItem(storageKey, JSON.stringify(state)) } catch { /* Storage is optional. */ }
  }, [state, storageKey])
  useEffect(() => {
    if (state.timer.status !== 'RUNNING') return
    const updateClock = () => setNow(Date.now())
    const interval = window.setInterval(updateClock, TICK_MILLIS)
    window.addEventListener('focus', updateClock)
    document.addEventListener('visibilitychange', updateClock)
    return () => {
      window.clearInterval(interval)
      window.removeEventListener('focus', updateClock)
      document.removeEventListener('visibilitychange', updateClock)
    }
  }, [state.timer.status])

  const current = projectTimer(state.timer, now - state.startedAt)
  function toggle() {
    const at = Date.now()
    setNow(at)
    setState(previous => ({ startedAt: at, timer: {
      ...previous.timer, ...projectTimer(previous.timer, at - previous.startedAt),
      status: previous.timer.status === 'RUNNING' ? 'PAUSED' : 'RUNNING',
    } }))
  }
  function reset(focusMinutes = state.timer.focusMillis / MINUTE_MILLIS, breakMinutes = state.timer.breakMillis / MINUTE_MILLIS) {
    setState(initialState(focusMinutes, breakMinutes))
    setNow(Date.now())
  }

  return { ...current, status: state.timer.status, focusMinutes: state.timer.focusMillis / MINUTE_MILLIS,
    breakMinutes: state.timer.breakMillis / MINUTE_MILLIS, toggle, reset }
}
