import { useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { configureRoom, joinRoom, leaveRoom, leaveRoomOnExit, timerCommand } from './api'
import type { RoomConfiguration, RoomSnapshot, StudyRoom } from './api'
import { useStudyRoom } from './useStudyRoom'
import RoomTimerPanel from './RoomTimerPanel'
import { useAction } from '../../shared/api/useAction'
import { useAuth } from '../../shared/auth/useAuth'
import WindowPage from '../../shared/components/WindowPage'
import Button from '../../shared/components/Button'
import Dialog from '../../shared/components/Dialog'
import StatePanel from '../../shared/components/StatePanel'
import ActionNotice from '../../shared/components/ActionNotice'
import RoomWeatherPanel from './RoomWeatherPanel'
import RoomParticipants from './RoomParticipants'
import MusicPlayer from '../music/MusicPlayer'
import NowPlayingMini from '../music/NowPlayingMini'
import MusicProvider from '../music/MusicProvider'
import AtmosphereScene from './AtmosphereScene'
import WindowControls from './WindowControls'
import HiddenPanelTools from './HiddenPanelTools'
import RoomRolesPanel from './RoomRolesPanel'
import RoomTimerSettingsPanel from './RoomTimerSettingsPanel'
import QuizBotLauncher from '../quiz/QuizBotLauncher'
import { useRoomFullscreen } from './useRoomFullscreen'
import { usePanelVisibility } from './usePanelVisibility'
import { DEFAULT_ATMOSPHERE } from './weatherTheme'
import type { Atmosphere } from './weatherTheme'
import './studyCorner.css'
import './studyroom.css'

export default function StudyRoomPage() {
  const { id } = useParams()
  // Changing groups remounts the page, so a previous room's private state cannot linger.
  const { account } = useAuth()
  return <RoomPage key={`${account?.id}-${id}`} groupId={Number(id)} />
}

/**
 * Says what leaving will do, mirroring the server's rules: the last person out ends the session,
 * and a departing host hands over to the present co-host or else the most recently active person.
 */
function leaveMessage(room: StudyRoom, accountId?: number) {
  const others = room.participants.filter(person => person.studentId !== accountId)
  const rejoin = 'You can rejoin any time while the group is open.'
  if (!others.length) return `You're the last one here, so leaving ends the session and resets the timer. ${rejoin}`
  if (room.hostId === accountId) {
    const next = others.find(person => person.studentId === room.coHostId) ?? others[0]
    return `${next.name} will take over as host, and the session carries on without you. ${rejoin}`
  }
  const count = others.length === 1 ? `${others[0].name}, who is` : `the ${others.length} others`
  return `The session carries on for ${count} still here. ${rejoin}`
}

function RoomPage({ groupId }: { groupId: number }) {
  const valid = Number.isSafeInteger(groupId) && groupId > 0
  const [clientId] = useState(() => crypto.randomUUID())
  const resource = useStudyRoom(groupId, clientId, valid)
  const room = resource.snapshot?.room
  const action = useAction()
  const { account } = useAuth()
  const [atmosphere, setAtmosphere] = useState<Atmosphere>(DEFAULT_ATMOSPHERE)
  const fullscreen = useRoomFullscreen()
  const panel = usePanelVisibility()
  const [confirmation, setConfirmation] = useState<'leave' | 'reset' | null>(null)
  const joined = useRef(false)
  useEffect(() => { joined.current = resource.joinedHere }, [resource.joinedHere])
  useEffect(() => () => {
    if (joined.current) void leaveRoom(groupId, clientId).catch(() => {})
    // Closing a browser may interrupt this request; the server lease also expires.
  }, [groupId, clientId])
  const { markJoined, refresh } = resource
  useEffect(() => {
    // A reload or closed tab skips React cleanup, which would leave this tab listed until its lease expired.
    const leaveOnExit = () => { if (joined.current) void leaveRoomOnExit(groupId, clientId).catch(() => {}) }
    // A page restored from the back/forward cache has already left, so it returns as a preview.
    const restore = (event: PageTransitionEvent) => { if (event.persisted && joined.current) { markJoined(false); void refresh() } }
    window.addEventListener('pagehide', leaveOnExit)
    window.addEventListener('pageshow', restore)
    return () => {
      window.removeEventListener('pagehide', leaveOnExit)
      window.removeEventListener('pageshow', restore)
    }
  }, [groupId, clientId, markJoined, refresh])

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
    if (room && await change(() => timerCommand(groupId, 'RESET', room.version), 'The session was ended for everyone.')) {
      setConfirmation(null)
    }
  }

  function settings(input: RoomConfiguration) {
    return change(() => configureRoom(groupId, input), 'Room settings were saved.')
  }

  const canControl = !!room && room.canControl && (resource.joinedHere || room.leader)

  return <WindowPage title={room ? `${room.groupName} · study room` : 'Study room'}
    description="A private focus space for accepted group members."
    actions={<Link className="retro-button" to="/study-room">Back to study room</Link>}>
    <ActionNotice error={action.error} success={action.success} />
    <StatePanel loading={resource.loading} error={valid ? resource.error : 'This room link is invalid.'}
      onRetry={valid ? () => void resource.refresh() : undefined} />
    {room && resource.snapshot && <>
      {resource.reconnecting && <p className="group-room-notices" role="status">
        <strong>Connection lost.</strong> Reconnecting to the room; the shared timer keeps running.
      </p>}
      <MusicProvider>
        <div className={`study-room-workspace atmosphere-workspace group-room-workspace${fullscreen.expanded ? ' is-expanded' : ''}${panel.hidden ? ' is-minimal' : ''}`}>
          <HiddenPanelTools panel={panel} fullscreen={fullscreen} />
          <AtmosphereScene className="atmosphere-backdrop" {...atmosphere} />
          <section className="group-room study-corner-panel" aria-labelledby="group-room-heading">
            <header className="group-room-header">
              <div className="room-corner-tools">
                <NowPlayingMini personal />
                <QuizBotLauncher home={{ scope: 'GROUP', groupId }}
                  canStart={room.leader || (!!account && (room.hostId === account.id || room.coHostId === account.id))} />
              </div>
              <div className="group-room-title">
                <span className={`group-room-tag${resource.joinedHere ? ' is-joined' : ''}`}>{resource.joinedHere ? 'In group' : 'Room preview'}</span>
                <h2 id="group-room-heading">{room.groupName}</h2>
              </div>
              <WindowControls panel={panel} fullscreen={fullscreen} tabs={[
                { id: 'timer', label: 'Timer', icon: '◷',
                  content: <RoomTimerSettingsPanel room={room} canEdit={canControl} pending={action.pending} onSave={settings} serverErrors={action.errors} /> },
                { id: 'atmosphere', label: 'Room atmosphere', icon: '☼', persistent: true,
                  content: <RoomWeatherPanel liveEnabled onChange={setAtmosphere} /> },
                { id: 'roles', label: 'Room roles', icon: '★',
                  content: <RoomRolesPanel room={room} canEdit={canControl} pending={action.pending} onSave={settings} serverErrors={action.errors} /> },
                { id: 'music', label: 'Music', icon: '♪', keepAfterOpen: true, content: <MusicPlayer /> },
              ]} />
            </header>
            <RoomParticipants room={room} accountId={account?.id} joinedHere={resource.joinedHere} />
            <RoomTimerPanel snapshot={resource.snapshot} canControl={canControl} joined={resource.joinedHere}
              pending={action.pending} onControl={control} />
            <footer className="group-room-footer">
              {!resource.joinedHere && room.joined && <p className="group-room-quiet"><strong>You are in this room in another tab.</strong> Join here to study from this tab instead, or close the other tab.</p>}
              {!room.participants.length && <p className="group-room-quiet"><strong>The room is quiet.</strong> Join when you are ready. Other group members can join from the group page.</p>}
              {resource.joinedHere
                ? <Button variant="danger" disabled={action.pending} onClick={() => setConfirmation('leave')}>Leave room</Button>
                : <Button variant="primary" disabled={action.pending} onClick={() => void join()}>Join study room</Button>}
            </footer>
          </section>
        </div>
      </MusicProvider>
      <div className="group-room-extras">
        <div className="actions"><Link className="retro-button" to={`/groups/${groupId}/sessions`}>View group sessions</Link></div>
      </div>
    </>}
    {confirmation && <Dialog title={confirmation === 'leave' ? 'Leave study room?' : 'End this session?'}
      busy={action.pending} onClose={() => setConfirmation(null)}>
      <p>{confirmation === 'leave' ? room ? leaveMessage(room, account?.id) : '' : `The timer goes back to ${room ? `${room.focusMinutes}:00` : 'the start'} for everyone. Everyone stays in the room.`}</p>
      <ActionNotice error={action.error} />
      <div className="actions">
        <Button disabled={action.pending} onClick={() => setConfirmation(null)}>Keep studying</Button>
        <Button variant="danger" disabled={action.pending} onClick={() => void (confirmation === 'leave' ? leave() : reset())}>
          {confirmation === 'leave' ? 'Leave study room' : 'End session for everyone'}
        </Button>
      </div>
    </Dialog>}
  </WindowPage>
}
