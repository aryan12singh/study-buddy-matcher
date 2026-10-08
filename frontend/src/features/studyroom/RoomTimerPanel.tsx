import { useEffect, useState } from 'react'
import type { RoomSnapshot } from './api'
import { projectTimer, timerText } from './timer'
import Button from '../../shared/components/Button'

type Command = 'START' | 'PAUSE' | 'RESUME' | 'RESET'
export default function RoomTimerPanel({ snapshot, canControl, pending, onControl }: {
  snapshot: RoomSnapshot; canControl: boolean; pending: boolean; onControl: (command: Command) => void
}) {
  const [now, setNow] = useState(snapshot.receivedAt)
  useEffect(() => {
    const interval = window.setInterval(() => setNow(performance.now()), 250)
    return () => window.clearInterval(interval)
  }, [])
  const timer = snapshot.room.timer
  const current = projectTimer(timer, Math.max(0, now - snapshot.receivedAt) + snapshot.estimatedLatency)
  return <section className="detail-panel room-timer-panel" aria-label="Shared Pomodoro timer">
    <p className="eyebrow">{current.phase === 'FOCUS' ? 'Focus time' : 'Break time'}</p>
    <p className="room-clock" role="timer" aria-label={`${current.phase === 'FOCUS' ? 'Focus' : 'Break'} remaining ${timerText(current.remainingMillis)}`}>{timerText(current.remainingMillis)}</p>
    <p>{timer.status === 'IDLE' ? 'Ready to start' : timer.status === 'PAUSED' ? 'Paused for everyone' : 'Running for everyone'}</p>
    <p className="muted">{snapshot.room.focusMinutes} {snapshot.room.focusMinutes === 1 ? 'minute' : 'minutes'} of focus · {snapshot.room.breakMinutes} {snapshot.room.breakMinutes === 1 ? 'minute' : 'minutes'} of break</p>
    {canControl ? <div className="actions">
      {timer.status === 'IDLE' && <Button variant="primary" disabled={pending} onClick={() => onControl('START')}>Start focus</Button>}
      {timer.status === 'RUNNING' && <Button disabled={pending} onClick={() => onControl('PAUSE')}>Pause for everyone</Button>}
      {timer.status === 'PAUSED' && <Button variant="primary" disabled={pending} onClick={() => onControl('RESUME')}>Resume for everyone</Button>}
      {timer.status !== 'IDLE' && <Button disabled={pending} onClick={() => onControl('RESET')}>Reset timer</Button>}
    </div> : <p className="muted">Join the room as its host or co-host to control the timer. The group leader can also control it.</p>}
  </section>
}
