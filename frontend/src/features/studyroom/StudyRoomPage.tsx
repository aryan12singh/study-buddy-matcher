import { useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { configureRoom, joinRoom, leaveRoom, selectAudio, timerCommand } from './api'
import type { AudioPreset, PresenceState, RoomConfiguration, RoomSnapshot } from './api'
import { useStudyRoom } from './useStudyRoom'
import RoomTimerPanel from './RoomTimerPanel'
import RoomAudioPanel from './RoomAudioPanel'
import RoomSettingsForm from './RoomSettingsForm'
import { useAction } from '../../shared/api/useAction'
import { useAuth } from '../../shared/auth/useAuth'
import WindowPage from '../../shared/components/WindowPage'
import Button from '../../shared/components/Button'
import Badge from '../../shared/components/Badge'
import Avatar from '../../shared/components/Avatar'
import Field from '../../shared/components/Field'
import Dialog from '../../shared/components/Dialog'
import StatePanel from '../../shared/components/StatePanel'
import ActionNotice from '../../shared/components/ActionNotice'
import './studyroom.css'

export default function StudyRoomPage() {
  const { id } = useParams()
  // Changing groups remounts the page, so a previous room's private state cannot linger.
  return <RoomPage key={id} groupId={Number(id)} />
}

function RoomPage({ groupId }: { groupId: number }) {
  const valid = Number.isSafeInteger(groupId) && groupId > 0
  const [clientId] = useState(() => crypto.randomUUID())
  const resource = useStudyRoom(groupId, clientId, valid)
  const room = resource.snapshot?.room
  const action = useAction()
  const { account } = useAuth()
  const [presence, setPresence] = useState<PresenceState>('PRESENT')
  const [confirmation, setConfirmation] = useState<'leave' | 'reset' | null>(null)
  const joined = useRef(false)
  useEffect(() => { joined.current = resource.joinedHere }, [resource.joinedHere])
  useEffect(() => () => {
    if (joined.current) void leaveRoom(groupId, clientId).catch(() => {})
    // Closing a browser may interrupt this request; the server lease also expires.
  }, [groupId, clientId])

  async function change(operation: () => Promise<RoomSnapshot>, message: string) {
    const result = await action.run(operation, message)
    if (result.ok) resource.accept(result.value)
    else void resource.refresh()
    return result.ok
  }

  async function join() {
    const result = await action.run(() => joinRoom(groupId, clientId), 'You joined the study room.')
    if (result.ok) {
      resource.markJoined(true)
      resource.setPresence('PRESENT')
      setPresence('PRESENT')
      resource.accept(result.value)
    }
  }

  async function leave() {
    const result = await action.run(async () => {
      await leaveRoom(groupId, clientId)
      // Switch to preview before the shared mutation event can renew the deleted lease.
      resource.markJoined(false)
    }, 'You left the room in this browser.')
    if (result.ok) {
      setConfirmation(null)
      void resource.refresh()
    }
  }

  function control(command: 'START' | 'PAUSE' | 'RESUME' | 'RESET') {
    if (!room) return
    if (command === 'RESET') { setConfirmation('reset'); return }
    void change(() => timerCommand(groupId, command, room.version), 'The shared timer was updated.')
  }

  async function reset() {
    if (room && await change(() => timerCommand(groupId, 'RESET', room.version), 'The shared timer was reset.')) {
      setConfirmation(null)
    }
  }

  function audio(preset: AudioPreset, playing: boolean) {
    if (room) void change(() => selectAudio(groupId, preset, playing, room.version), 'The room audio was updated.')
  }

  function settings(input: RoomConfiguration) {
    return change(() => configureRoom(groupId, input), 'Room settings were saved.')
  }

  return <WindowPage title={room ? `${room.groupName} · study room` : 'Study room'}
    description="A private focus space for accepted group members."
    actions={<Link className="retro-button" to={valid ? `/groups/${groupId}` : '/groups'}>Back to group</Link>}>
    <ActionNotice error={action.error} success={action.success} />
    <StatePanel loading={resource.loading} error={valid ? resource.error : 'This room link is invalid.'}
      onRetry={valid ? () => void resource.refresh() : undefined} />
    {room && resource.snapshot && <>
      <section className="detail-panel room-entry">
        <div>
          <div className="tag-list">
            <Badge tone={resource.joinedHere ? 'good' : 'neutral'}>{resource.joinedHere ? 'You are in the room' : 'Room preview'}</Badge>
            <Badge>{room.participants.length} / {room.participantLimit} present</Badge>
          </div>
          <p className="muted">Presence expires after {Math.round(room.leaseLifetimeMillis / 1000)} seconds without a heartbeat. The shared timer continues when people disconnect.</p>
        </div>
        <div className="actions">
          {resource.joinedHere
            ? <Button disabled={action.pending} onClick={() => setConfirmation('leave')}>Leave room</Button>
            : <Button variant="primary" disabled={action.pending} onClick={() => void join()}>Join study room</Button>}
        </div>
      </section>
      {!room.hostOnline && <p className="privacy-note">The host is away. The leader retains controls, and a present co-host can keep the session going.</p>}
      <div className="room-layout">
        <RoomTimerPanel snapshot={resource.snapshot} canControl={room.canControl && (resource.joinedHere || room.leader)}
          pending={action.pending} onControl={control} />
        <section className="detail-panel">
          <h2>People here</h2>
          <StatePanel empty={!room.participants.length} emptyTitle="The room is quiet"
            emptyMessage="Join when you are ready. Other group members can join from the group page." emptyKind="groups" />
          {!!room.participants.length && <ul className="room-participants">
            {room.participants.map(person => <li key={person.studentId} className="data-row">
              <div>
                <div className="person-heading"><Avatar name={person.name} small /><span>{person.name}{person.studentId === account?.id ? ' (you)' : ''}</span></div>
                <div className="tag-list">
                  {person.leader && <Badge>Leader</Badge>}{person.host && <Badge>Host</Badge>}{person.coHost && <Badge>Co-host</Badge>}
                  <Badge tone={person.presence === 'FOCUS' ? 'good' : 'neutral'}>{person.presence === 'FOCUS' ? 'Focusing' : person.presence === 'BREAK' ? 'On a break' : 'Present'}</Badge>
                </div>
              </div>
            </li>)}
          </ul>}
          {resource.joinedHere && <Field id="room-presence" label="Your status">
            <select id="room-presence" value={presence} onChange={event => {
              const value = event.target.value as PresenceState
              setPresence(value); resource.setPresence(value); void resource.refresh()
            }}>
              <option value="PRESENT">Present</option><option value="FOCUS">Focusing</option><option value="BREAK">On a break</option>
            </select>
          </Field>}
          <p className="privacy-note">Group membership shares room access, not contact numbers.</p>
        </section>
      </div>
      <RoomAudioPanel key={resource.joinedHere ? 'joined' : 'preview'} room={room} active={resource.joinedHere} pending={action.pending} onSelect={audio} />
      {room.leader && <RoomSettingsForm room={room} pending={action.pending} onSave={settings} serverErrors={action.errors} />}
    </>}
    {confirmation && <Dialog title={confirmation === 'leave' ? 'Leave study room?' : 'Reset shared timer?'}
      busy={action.pending} onClose={() => setConfirmation(null)}>
      <p>{confirmation === 'leave' ? 'This browser will leave and stop its audio. You can join again while the group is open.' : 'This resets the focus cycle for everyone in the room.'}</p>
      <ActionNotice error={action.error} />
      <div className="actions">
        <Button disabled={action.pending} onClick={() => setConfirmation(null)}>Keep studying</Button>
        <Button variant="danger" disabled={action.pending} onClick={() => void (confirmation === 'leave' ? leave() : reset())}>
          {confirmation === 'leave' ? 'Leave study room' : 'Reset for everyone'}
        </Button>
      </div>
    </Dialog>}
  </WindowPage>
}
