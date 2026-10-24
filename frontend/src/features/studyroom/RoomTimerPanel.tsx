import { useEffect, useRef, useState } from 'react'
import type { RoomSnapshot, TimerPhase } from './api'
import { projectTimer, timerText } from './timer'
import Button from '../../shared/components/Button'
import PixelIcon from '../../shared/components/PixelIcon'

type Command = 'START' | 'PAUSE' | 'RESUME' | 'RESET'
const TICK_MILLIS = 250
const MAXIMUM_OBSERVATION_GAP_MILLIS = 1000

function continuesTimer(previous: RoomSnapshot, next: RoomSnapshot) {
  if (previous.room.groupId !== next.room.groupId || previous.room.timer.status !== 'RUNNING' || next.room.timer.status !== 'RUNNING'
    || previous.room.timer.focusMillis !== next.room.timer.focusMillis || previous.room.timer.breakMillis !== next.room.timer.breakMillis) return false
  if (previous === next) return true
  const elapsed = Date.parse(next.room.serverTime) - Date.parse(previous.room.serverTime)
  if (!Number.isFinite(elapsed) || elapsed < 0) return false
  const expected = projectTimer(previous.room.timer, elapsed)
  // JSON dates and duration milliseconds each discard sub-millisecond precision.
  return expected.phase === next.room.timer.phase && Math.abs(expected.remainingMillis - next.room.timer.remainingMillis) <= 2
}

export default function RoomTimerPanel({ snapshot, canControl, joined = true, pending, onControl }: {
  snapshot: RoomSnapshot; canControl: boolean; joined?: boolean; pending: boolean; onControl: (command: Command) => void
}) {
  const [now, setNow] = useState(snapshot.receivedAt)
  const [transition, setTransition] = useState('')
  const observed = useRef<{ snapshot: RoomSnapshot; phase: TimerPhase; phaseStartedAt: number; at: number } | null>(null)
  useEffect(() => {
    const interval = window.setInterval(() => setNow(performance.now()), TICK_MILLIS)
    const resetObservation = () => { observed.current = null; setTransition('') }
    window.addEventListener('focus', resetObservation)
    document.addEventListener('visibilitychange', resetObservation)
    return () => {
      window.clearInterval(interval)
      window.removeEventListener('focus', resetObservation)
      document.removeEventListener('visibilitychange', resetObservation)
    }
  }, [])
  const timer = snapshot.room.timer
  const renderedAt = Math.max(now, snapshot.receivedAt)
  const elapsed = renderedAt - snapshot.receivedAt + snapshot.estimatedLatency
  const current = projectTimer(timer, elapsed)
  const phaseStartedAt = Date.parse(snapshot.room.serverTime) + elapsed + current.remainingMillis
    - (current.phase === 'FOCUS' ? timer.focusMillis : timer.breakMillis)
  useEffect(() => {
    const previous = observed.current
    const continuing = previous && !document.hidden && renderedAt - previous.at <= MAXIMUM_OBSERVATION_GAP_MILLIS
      && continuesTimer(previous.snapshot, snapshot)
    if (continuing && previous.phase !== current.phase && phaseStartedAt > previous.phaseStartedAt + 2) {
      setTransition(current.phase === 'BREAK' ? 'Break started. Take a moment to recharge.' : 'Focus started. Time to study together.')
    } else if (previous && !continuing && (previous.snapshot !== snapshot || timer.status === 'RUNNING')) {
      setTransition('')
    }
    // A new latency estimate can briefly render the previous phase again. Keep
    // the latest observed boundary so a refresh cannot reverse or repeat its cue.
    const correctingBackwards = continuing && phaseStartedAt < previous.phaseStartedAt - 2
    observed.current = document.hidden ? null : { snapshot, at: renderedAt,
      phase: correctingBackwards ? previous.phase : current.phase,
      phaseStartedAt: correctingBackwards ? previous.phaseStartedAt : phaseStartedAt }
  }, [current.phase, phaseStartedAt, renderedAt, snapshot, timer.status])
  const focusing = current.phase === 'FOCUS'
  const duration = focusing ? timer.focusMillis : timer.breakMillis
  const progress = duration > 0 ? Math.max(0, Math.min(100, (1 - current.remainingMillis / duration) * 100)) : 0
  return <section className="room-timer-panel" aria-label="Shared Pomodoro timer">
    <p className="personal-pomodoro-clock" role="timer" aria-live="off" aria-label={`${focusing ? 'Focus' : 'Break'} remaining ${timerText(current.remainingMillis)}`}>{timerText(current.remainingMillis)}</p>
    <p className="room-timer-cue" role="status" aria-live="polite" aria-atomic="true">{transition}</p>
    <div className="pomodoro-journey">
      <div className="pomodoro-track-labels"><span>{focusing ? 'A little closer with every minute' : 'A well-earned breather'}</span><span>FINISH</span></div>
      <div className="pomodoro-track" role="progressbar" aria-label={`${focusing ? 'Focus' : 'Break'} progress`}
        aria-valuemin={0} aria-valuemax={100} aria-valuenow={Math.round(progress)}>
        <span className="pomodoro-track-fill" style={{ width: `${progress}%` }} />
        <span className="pomodoro-traveller" data-running={timer.status === 'RUNNING'} style={{ left: `${progress}%` }} aria-hidden="true"><PixelIcon kind="groups" /></span>
        <span className="pomodoro-finish" aria-hidden="true" />
      </div>
      <p className="pomodoro-rhythm">{snapshot.room.focusMinutes} {snapshot.room.focusMinutes === 1 ? 'minute' : 'minutes'} of focus<span aria-hidden="true"> · </span>{snapshot.room.breakMinutes} {snapshot.room.breakMinutes === 1 ? 'minute' : 'minutes'} of break</p>
    </div>
    {joined && (canControl ? <div className="actions room-timer-actions">
      {timer.status === 'IDLE' && <Button variant="primary" disabled={pending} onClick={() => onControl('START')}>Start focus</Button>}
      {timer.status === 'RUNNING' && <Button disabled={pending} onClick={() => onControl('PAUSE')}>Pause for everyone</Button>}
      {timer.status === 'PAUSED' && <Button variant="primary" disabled={pending} onClick={() => onControl('RESUME')}>Resume for everyone</Button>}
      {timer.status !== 'IDLE' && <Button disabled={pending} onClick={() => onControl('RESET')}>End session</Button>}
    </div> : <p className="room-timer-note">Only the host, co-host or group leader can control the timer.</p>)}
  </section>
}
