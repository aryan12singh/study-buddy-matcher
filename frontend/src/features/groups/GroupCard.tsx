import { Link } from 'react-router-dom'
import type { GroupSummary } from './api'
import { label } from '../../shared/api/types'
import Badge from '../../shared/components/Badge'

export default function GroupCard({ group }: { group: GroupSummary }) {
  return (
    <article className="data-row">
      <div>
        <h2>
          <Link to={`/groups/${group.id}`}>{group.name}</Link>
        </h2>
        <p>{group.courseCode} — {group.courseName}</p>
        <p className="row-meta">Led by {group.leaderName} · {label(group.preferredStudyMode)}</p>
        <div className="tag-list">
          <Badge tone={group.active ? 'good' : 'neutral'}>{group.active ? `${group.memberCount}/${group.maxGroupSize} members` : 'Closed group'}</Badge>
          {group.viewer.leader ? (
            <Badge>Group leader</Badge>
          ) : group.viewer.member ? (
            <Badge tone="good">Member</Badge>
          ) : group.viewer.requestStatus === 'PENDING' ? (
            <Badge tone="pending">Application pending</Badge>
          ) : group.active && group.memberCount >= group.maxGroupSize ? (
            <Badge>Full</Badge>
          ) : null}
          {group.studyGoals.map(goal => (
            <Badge key={goal}>{label(goal)}</Badge>
          ))}
        </div>
      </div>
      <div className="actions">
        <Link className="retro-button" to={`/groups/${group.id}`}>View group</Link>
        {group.viewer.leader && (
          <Link className="retro-button" to={`/groups/${group.id}/manage`}>Manage group</Link>
        )}
      </div>
    </article>
  )
}
