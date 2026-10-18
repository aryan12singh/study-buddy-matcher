import { Link, useLocation } from 'react-router-dom'
import type { MatchResult } from './api'
import { QUALITY_LABELS, QUALITY_TONES, sharedHoursText } from './matchSearchForm'
import { label } from '../../shared/api/types'
import Avatar from '../../shared/components/Avatar'
import Badge from '../../shared/components/Badge'
import Button from '../../shared/components/Button'
import ScoreBreakdown from './ScoreBreakdown'
import SharedHoursHeatmap from './SharedHoursHeatmap'
import { returnToState } from '../../shared/navigation/returnTo'

/** One search result: who the student is, why they match, and when you are both free. */
export default function MatchCard({ match, onSendRequest }: { match: MatchResult; onSendRequest: () => void }) {
  const location = useLocation()
  const backToSearch = returnToState(`${location.pathname}${location.search}`, 'Back to find buddies')
  return (
    <article className={`match-card match-${match.quality.toLowerCase()}`}>
      <div className="match-card-main">
        <div className="person-heading">
          <Avatar name={match.name} />
          <div>
            <h3><Link to={`/students/${match.studentId}`} state={backToSearch}>{match.name}</Link> <Badge>Year {match.yearOfStudy}</Badge></h3>
            <p className="row-meta">{match.programme} · {match.school}</p>
          </div>
        </div>
        <p className="match-compatibility"><strong>{new Intl.NumberFormat('en', { style: 'percent', maximumFractionDigits: 1 }).format(match.totalScore)} compatibility</strong><span className="field-hint">A comparison of study preferences, not a probability of success.</span></p>
        {match.relevantCourse && <p className="row-meta">Relevant course: {match.relevantCourse.code} {match.relevantCourse.name}</p>}
        <ScoreBreakdown match={match} />
        <div className="match-card-footer">
          <p className="row-meta">{match.relationship === 'CONNECTED' ?
            'You are connected. Their contact number is on their profile.'
            : 'Contact number shared once a request is accepted'}</p>
          <div className="actions">
            <Link className="retro-button" to={`/students/${match.studentId}`} state={backToSearch}>View profile</Link>
            <RequestAction match={match} onSendRequest={onSendRequest} />
          </div>
        </div>
      </div>
      <div className="match-card-time">
        <Badge tone={QUALITY_TONES[match.quality]}>{QUALITY_LABELS[match.quality]}</Badge>
        <h4>Shared hours</h4>
        <SharedHoursHeatmap slots={match.sharedSlots} />
        <p className="row-meta">{sharedHoursText(match.sharedHoursPerWeek)}</p>
        <ul className="tag-list" aria-label="Study preferences">
          {match.studyMode && <li className="status-badge">{label(match.studyMode)}</li>}
          {match.studyGoals.map(goal => <li key={goal} className="status-badge">{label(goal)}</li>)}
        </ul>
      </div>
    </article>
  )
}

/** Offers a request only when none is pending and the students are not already connected. */
function RequestAction({ match, onSendRequest }: { match: MatchResult; onSendRequest: () => void }) {
  switch (match.relationship) {
    case 'OUTGOING_PENDING':
      return <Button disabled title="Waiting for their reply">Match requested</Button>
    case 'INCOMING_PENDING':
      return <Link className="retro-button primary" to="/connections?view=incoming">Respond to their request</Link>
    case 'CONNECTED':
      return <Badge tone="good">Connected</Badge>
    default:
      return <Button variant="primary" onClick={onSendRequest}>Send match request</Button>
  }
}
