import type { RoomConfiguration, StudyRoom } from './api'
import RoomSettingsForm from './RoomSettingsForm'

const minutes = (value: number) => `${value} ${value === 1 ? 'minute' : 'minutes'}`

export default function RoomTimerSettingsPanel({ room, canEdit, pending, onSave, serverErrors }: {
  room: StudyRoom; canEdit: boolean; pending: boolean
  onSave: (input: RoomConfiguration) => Promise<boolean>; serverErrors: Record<string, string>
}) {
  return <div className="room-timer-settings-panel">
    <dl className="room-roles" aria-label="Current timer">
      <div><dt>Focus</dt><dd>{minutes(room.focusMinutes)}</dd></div>
      <div><dt>Break</dt><dd>{minutes(room.breakMinutes)}</dd></div>
    </dl>
    {canEdit
      ? <RoomSettingsForm room={room} fields="timer" pending={pending} onSave={onSave} serverErrors={serverErrors} />
      : <p className="muted">The host, co-host or group leader can change the timer for everyone.</p>}
  </div>
}
