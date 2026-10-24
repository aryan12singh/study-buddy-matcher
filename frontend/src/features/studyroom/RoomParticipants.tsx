import type { CSSProperties } from 'react'
import type { PresenceState, StudyRoom } from './api'
import Avatar from '../../shared/components/Avatar'

const PRESENCE_LABELS: Record<PresenceState, string> = { PRESENT: 'Here', FOCUS: 'Focusing', BREAK: 'On a break' }

/**
 * The people in the room as a little crew inside the timer panel. Each avatar bobs gently, out of
 * step with the others; a dot shows whether they are focusing or on a break, and a badge marks
 * the leader and hosts. Motion stops when the student prefers reduced motion.
 */
export default function RoomParticipants({ room, accountId, joinedHere }: {
  room: StudyRoom; accountId?: number; joinedHere: boolean
}) {
  if (!room.participants.length) return null
  return (
    <section className="room-crew" aria-label="Studying together">
      <p className="room-crew-heading">
        <span>Studying together</span>
        <span className="room-crew-count">{room.participants.length} / {room.participantLimit}</span>
      </p>
      <ul className="room-crew-list">
        {room.participants.map((person, index) => {
          const roles = [person.leader && 'Leader', person.host && 'Host', person.coHost && 'Co-host'].filter(Boolean).join(' · ')
          const you = person.studentId !== accountId ? '' : joinedHere ? ' (you)' : ' (you, in another tab)'
          return (
            <li key={person.studentId} className={`room-crew-member is-${person.presence.toLowerCase()}`}
              style={{ '--crew-delay': `${index * -0.45}s` } as CSSProperties}>
              <span className="room-crew-avatar">
                <Avatar name={person.name} />
                <span className="room-crew-dot" aria-hidden="true" />
                {roles && <span className="room-crew-badge" aria-hidden="true">{person.leader ? '♛' : '★'}</span>}
              </span>
              <span className="room-crew-name">{person.name}{you}</span>
              <span className="room-crew-role">{roles || PRESENCE_LABELS[person.presence]}</span>
              {roles && <span className="visually-hidden">{PRESENCE_LABELS[person.presence]}</span>}
            </li>
          )
        })}
      </ul>
    </section>
  )
}
