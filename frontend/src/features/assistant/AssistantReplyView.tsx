import { Link } from 'react-router-dom'
import Badge from '../../shared/components/Badge'
import Button from '../../shared/components/Button'
import { label } from '../../shared/api/types'
import type { AssistantReply } from './api'

/**
 * What a reply shows below its words: the filters it understood, real match cards linking to each
 * student's profile, the group's shared free times, what is waiting for the student, and the next
 * step it offers.
 */
export default function AssistantReplyView({ reply, onOpenQuizBuddy, onNavigate }: {
  reply: AssistantReply; onOpenQuizBuddy: () => void; onNavigate: () => void
}) {
  const action = reply.action
  return <div className="assistant-reply-extras">
    {!!reply.filters.length && <ul className="assistant-filters" aria-label="Filters used">
      {reply.filters.map(filter => <li key={filter}>{filter}</li>)}
    </ul>}
    {!!reply.matches.length && <ul className="assistant-matches" aria-label="Matches">
      {reply.matches.map(match => <li key={match.studentId} className="assistant-match">
        <div className="assistant-match-heading">
          <Link to={`/students/${match.studentId}`} onClick={onNavigate}><strong>{match.name}</strong></Link>
          <Badge tone={match.quality === 'STRONG' ? 'good' : 'neutral'}>{label(match.quality)}</Badge>
        </div>
        <p>{match.programme}, year {match.yearOfStudy} · {match.sharedHoursPerWeek} shared {match.sharedHoursPerWeek === 1 ? 'hour' : 'hours'} a week</p>
      </li>)}
    </ul>}
    {!!reply.slots.length && <ul className="assistant-slots" aria-label="Shared free times">
      {reply.slots.map(slot => <li key={`${slot.dayOfWeek}-${slot.startTime}`}>
        <strong>{label(slot.dayOfWeek)}</strong> {slot.startTime.slice(0, 5)}–{slot.endTime.slice(0, 5)}
      </li>)}
    </ul>}
    {!!reply.todos.length && <ul className="assistant-todos" aria-label="Waiting for you">
      {reply.todos.map(todo => <li key={todo.text}>
        <span>{todo.text}</span>
        <Link to={todo.link} onClick={onNavigate}>{todo.label} ›</Link>
      </li>)}
    </ul>}
    {action && (action.kind === 'OPEN_QUIZ_BUDDY' || !action.link
      ? <Button variant="primary" onClick={onOpenQuizBuddy}>{action.label}</Button>
      : <Link className="retro-button primary" to={action.link} onClick={onNavigate}>{action.label}</Link>)}
  </div>
}
