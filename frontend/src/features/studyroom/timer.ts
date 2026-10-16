import type { RoomTimer } from './api'

/** Client rendering only; every control and refresh still uses server state. */
export function projectTimer(timer: RoomTimer, elapsedMillis: number) {
  if (timer.status !== 'RUNNING') return { phase: timer.phase, remainingMillis: timer.remainingMillis }
  let elapsed = Math.max(0, elapsedMillis)
  if (elapsed < timer.remainingMillis) return { phase: timer.phase, remainingMillis: timer.remainingMillis - elapsed }
  elapsed = (elapsed - timer.remainingMillis) % (timer.focusMillis + timer.breakMillis)
  let phase = timer.phase === 'FOCUS' ? 'BREAK' as const : 'FOCUS' as const
  const duration = phase === 'FOCUS' ? timer.focusMillis : timer.breakMillis
  if (elapsed >= duration) { elapsed -= duration; phase = phase === 'FOCUS' ? 'BREAK' : 'FOCUS' }
  return { phase, remainingMillis: (phase === 'FOCUS' ? timer.focusMillis : timer.breakMillis) - elapsed }
}

export function timerText(milliseconds: number) {
  const seconds = Math.max(0, Math.ceil(milliseconds / 1000))
  return `${Math.floor(seconds / 60).toString().padStart(2, '0')}:${(seconds % 60).toString().padStart(2, '0')}`
}
