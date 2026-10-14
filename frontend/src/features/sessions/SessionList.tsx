import { Link } from 'react-router-dom'
import type { StudySession } from './api'
import { occurrenceLabel } from './sessionPresentation'
import Badge from '../../shared/components/Badge'
import StatePanel from '../../shared/components/StatePanel'

export default function SessionList({ sessions }: { sessions: StudySession[] }) {
  return <>
    <StatePanel empty={!sessions.length} emptyTitle="No study sessions yet" emptyMessage="Plan a dated session with your group or a connected buddy." emptyKind="clock" />
    {!!sessions.length && <ul className="data-list session-list" aria-label="Study sessions">
      {sessions.map(session => <li key={session.id} className="data-row">
        <div>
          <h3><Link to={`/sessions/${session.id}`}>{session.title}</Link></h3>
          <div className="tag-list"><Badge tone={session.status === 'SCHEDULED' ? 'good' : 'neutral'}>{session.status === 'CANCELLED' ? 'Cancelled' : session.nextOccurrence ? 'Upcoming' : 'Past session'}</Badge>
            <Badge>{session.scope === 'GROUP' ? 'Group' : 'Buddy'}</Badge>
            {session.occurrenceCount > 1 && <Badge>{session.occurrenceCount} weekly occurrences</Badge>}</div>
          <p>{occurrenceLabel(session.nextOccurrence ?? { startAt: session.startAt, endAt: session.endAt }, session.timeZone)}</p>
          <p className="row-meta">{session.timeZone}{session.location ? ` · ${session.location}` : ''}</p>
        </div>
        <Link className="retro-button" to={`/sessions/${session.id}`}>View session</Link>
      </li>)}
    </ul>}
  </>
}
