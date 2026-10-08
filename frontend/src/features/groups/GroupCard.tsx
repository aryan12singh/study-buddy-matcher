import { Link } from 'react-router-dom'
import type { GroupSummary } from './api'
import { label } from '../../shared/api/types'
import Badge from '../../shared/components/Badge'
import CapacityMeter from '../../shared/components/CapacityMeter'
import Avatar from '../../shared/components/Avatar'

export default function GroupCard({ group }: { group: GroupSummary }) {
  return (
    <article className="data-row group-card">
      <div>
        <span className="course-tag">{group.courseCode}</span>
        <h2>
          <Link to={`/groups/${group.id}`}>{group.name}</Link>
        </h2>
        <p className="muted">{group.courseName}</p>
        <p className="leader-caption"><Avatar name={group.leaderName} small /><span>Led by {group.leaderName} · {label(group.preferredStudyMode)}</span></p>
        <div className="tag-list">
          {!group.active && <Badge>Closed group</Badge>}
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
        <CapacityMeter members={group.memberCount} capacity={group.maxGroupSize} active={group.active} />
      </div>
      <div className="actions">
        <Link className={`retro-button${group.viewer.leader ? '' : ' primary'}`} to={`/groups/${group.id}`}>View group</Link>
        {group.viewer.leader && group.active && (
          <Link className="retro-button primary" to={`/groups/${group.id}/manage`}>Manage group</Link>
        )}
      </div>
    </article>
  )
}
