import { useState } from 'react'
import type { ReactNode } from 'react'
import Button from '../../shared/components/Button'
import ConfirmDialog from '../../shared/components/ConfirmDialog'
import PixelIcon from '../../shared/components/PixelIcon'
import { MINUTE_MILLIS, usePersonalPomodoro } from './usePersonalPomodoro'
import { timerText } from './timer'
import { useTimerPings } from './timerPing'
import WindowControls from './WindowControls'
import type { RoomSettingsTab } from './RoomSettingsMenu'
import PersonalTimerSettings from './PersonalTimerSettings'
import type { PanelVisibility } from './usePanelVisibility'
import type { RoomFullscreen } from './useRoomFullscreen'

export default function PersonalPomodoro({ accountId, panel, fullscreen, extraTabs = [], corner }: {
  accountId: number; panel: PanelVisibility; fullscreen: RoomFullscreen; extraTabs?: RoomSettingsTab[]
  /** Shown in the top-left corner, opposite the window controls, such as the music mini player. */
  corner?: ReactNode
}) {
  const timer = usePersonalPomodoro(accountId)
  useTimerPings(timer.status, timer.phase)
  const [confirmingReset, setConfirmingReset] = useState(false)
  const focusing = timer.phase === 'FOCUS'
  const duration = (focusing ? timer.focusMinutes : timer.breakMinutes) * MINUTE_MILLIS
  const progress = Math.max(0, Math.min(100, (1 - timer.remainingMillis / duration) * 100))

  return (
    <section className="personal-pomodoro study-corner-panel" aria-label="Personal Pomodoro timer">
      <WindowControls panel={panel} fullscreen={fullscreen} tabs={[
        { id: 'timer', label: 'Timer', icon: '◷', content: <PersonalTimerSettings
          focusMinutes={timer.focusMinutes} breakMinutes={timer.breakMinutes} onSave={(focus, rest) => timer.reset(focus, rest)} /> },
        ...extraTabs,
      ]} />
      {corner}
      <div className="pomodoro-welcome">
        <span className="pomodoro-kicker"><PixelIcon kind="clock" />Your focus space</span>
        <h2>A little focus. A little progress.</h2>
        <p>Settle in, take a breath, and make time for what matters.</p>
      </div>
      <div className="pomodoro-center">
        <span className={`pomodoro-phase${focusing ? '' : ' is-break'}`}>{focusing ? 'Focus time' : 'Take a break'}</span>
        <p className="personal-pomodoro-clock" role="timer" aria-live="off"
          aria-label={`${focusing ? 'Focus' : 'Break'} remaining ${timerText(timer.remainingMillis)}`}>{timerText(timer.remainingMillis)}</p>
        <p className="pomodoro-caption" role="status" aria-live="polite">
          {timer.status === 'IDLE' ? 'Your next small step starts here.' : timer.status === 'PAUSED' ? 'Paused. Come back when you are ready.'
            : focusing ? 'One thing at a time. You have got this.' : 'Stretch, rest your eyes, and recharge.'}
        </p>
      </div>
      <div className="pomodoro-journey">
        <div className="pomodoro-track-labels"><span>{focusing ? 'A little closer with every minute' : 'A well-earned breather'}</span><span>{focusing ? 'FOCUS FINISH' : 'BREAK FINISH'}</span></div>
        <div className="pomodoro-track" role="progressbar" aria-label={`${focusing ? 'Focus' : 'Break'} progress`}
          aria-valuemin={0} aria-valuemax={100} aria-valuenow={Math.round(progress)}>
          <span className="pomodoro-track-fill" style={{ width: `${progress}%` }} />
          <span className="pomodoro-traveller" data-running={timer.status === 'RUNNING'} style={{ left: `${progress}%` }} aria-hidden="true"><PixelIcon kind="brand" /></span>
          <span className="pomodoro-finish" aria-hidden="true" />
        </div>
        <p className="pomodoro-rhythm">{timer.focusMinutes} min focus<span aria-hidden="true"> · </span>{timer.breakMinutes} min break</p>
      </div>
      <div className="pomodoro-bottom">
        <div className="actions">
          <Button variant="primary" onClick={timer.toggle}>{timer.status === 'RUNNING' ? 'Pause timer' : timer.status === 'PAUSED' ? 'Resume timer' : 'Start focus'}</Button>
          <Button disabled={timer.status === 'IDLE'} onClick={() => setConfirmingReset(true)}>End session</Button>
        </div>
      </div>
      {confirmingReset && <ConfirmDialog title="End this session?" confirmLabel="End session" pending={false}
        onClose={() => setConfirmingReset(false)} onConfirm={() => { timer.reset(); setConfirmingReset(false) }}>
        <p>Your timer goes back to {timer.focusMinutes}:00, ready for your next focus.</p>
      </ConfirmDialog>}
    </section>
  )
}
