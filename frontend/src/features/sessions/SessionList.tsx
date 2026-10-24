import { Link } from 'react-router-dom'
import type { StudySession } from './api'
import { displayedOccurrence, nextSessionOccurrence, occurrenceLabel, sessionStatusLabel } from './sessionPresentation'
import { useSessionClock } from './useSessionClock'
import SessionRoomLink from './SessionRoomLink'
import Badge from '../../shared/components/Badge'
import StatePanel from '../../shared/components/StatePanel'

export default function SessionList({ sessions, upcomingOnly = false, limit }: { sessions: StudySession[]; upcomingOnly?: boolean; limit?: number }) {
  const now = useSessionClock(sessions)
  const selected = upcomingOnly ? sessions.filter(session => nextSessionOccurrence(session, now))
    .sort((left, right) => Date.parse(nextSessionOccurrence(left, now)!.startAt) - Date.parse(nextSessionOccurrence(right, now)!.startAt)) : sessions
  const visible = limit === undefined ? selected : selected.slice(0, limit)
  return <>
    <StatePanel empty={!visible.length} emptyTitle={upcomingOnly ? 'No upcoming study sessions' : 'No study sessions yet'}
      emptyMessage="Plan a dated session with your group or a connected buddy." emptyKind="clock"
      emptyAction={<Link to="/help#help-sessions">How to plan sessions and export a calendar</Link>} />
    {!!visible.length && <ul className="data-list session-list" aria-label="Study sessions">
      {visible.map(session => <li key={session.id} className="data-row">
        <div>
          <h3><Link to={`/sessions/${session.id}`}>{session.title}</Link></h3>
          <div className="tag-list"><Badge tone={nextSessionOccurrence(session, now) ? 'good' : 'neutral'}>{sessionStatusLabel(session, now)}</Badge>
            <Badge>{session.scope === 'GROUP' ? 'Group' : 'Buddy'}</Badge>
            {session.occurrenceCount > 1 && <Badge>{session.occurrenceCount} weekly occurrences</Badge>}</div>
          <p>{occurrenceLabel(displayedOccurrence(session, now), session.timeZone)}</p>
          <p className="row-meta">{session.timeZone}{session.location ? ` · ${session.location}` : ''}</p>
        </div>
        <div className="actions"><Link className="retro-button" to={`/sessions/${session.id}`}>View session</Link>
          {session.scope === 'GROUP' && <SessionRoomLink session={session} now={now} />}</div>
      </li>)}
    </ul>}
  </>
}
