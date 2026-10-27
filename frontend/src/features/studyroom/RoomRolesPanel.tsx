import type { RoomConfiguration, StudyRoom } from './api'
import Avatar from '../../shared/components/Avatar'
import RoomSettingsForm from './RoomSettingsForm'

export default function RoomRolesPanel({ room, canEdit, pending, onSave, serverErrors }: {
  room: StudyRoom; canEdit: boolean; pending: boolean
  onSave: (input: RoomConfiguration) => Promise<boolean>; serverErrors: Record<string, string>
}) {
  const nameOf = (studentId: number | null) => room.members.find(member => member.studentId === studentId)?.name
  const host = nameOf(room.hostId)
  const coHost = nameOf(room.coHostId)

  return <div className="room-roles-panel">
    <dl className="room-roles" aria-label="Current room roles">
      <div><dt>Host</dt><dd>{host ? <><Avatar name={host} small />{host}</> : 'Not assigned'}</dd></div>
      <div><dt>Co-host</dt><dd>{coHost ? <><Avatar name={coHost} small />{coHost}</> : 'No co-host'}</dd></div>
      <div><dt>Capacity</dt><dd>{room.participantLimit} {room.participantLimit === 1 ? 'person' : 'people'}</dd></div>
    </dl>
    {canEdit
      ? <RoomSettingsForm room={room} fields="roles" pending={pending} onSave={onSave} serverErrors={serverErrors} />
      : <p className="muted">The group leader assigns the host and co-host. They control the shared timer and audio.</p>}
  </div>
}
