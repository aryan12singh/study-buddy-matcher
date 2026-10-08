import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getGroup } from './api'
import { useResource } from '../../shared/api/useResource'
import { label, formatTimestamp } from '../../shared/api/types'
import WindowPage from '../../shared/components/WindowPage'
import StatePanel from '../../shared/components/StatePanel'
import Badge from '../../shared/components/Badge'
import Button from '../../shared/components/Button'
import WeeklySchedule from '../../shared/components/WeeklySchedule'
import Dialog from '../../shared/components/Dialog'
import ApplyGroupDialog from './ApplyGroupDialog'
import Avatar from '../../shared/components/Avatar'
import CapacityMeter from '../../shared/components/CapacityMeter'
import CopyButton from '../../shared/components/CopyButton'

export default function GroupDetailPage() {
  const { id } = useParams(),
    groupId = Number(id),
    validId = Number.isSafeInteger(groupId) && groupId > 0
  const resource = useResource(`group-${id}`, signal => getGroup(groupId, signal), validId)
  const group = resource.data
  const [applicationGroup, setApplicationGroup] = useState<{ id: number; name: string } | null>(null)
  const [agenda, setAgenda] = useState(false)
  return (
    <WindowPage
      title={group?.name || 'Study group'}
      description={group ? `${group.courseCode} — ${group.courseName}` : undefined}
      actions={<Link className="retro-button" to="/groups">Back to groups</Link>}
    >
      <StatePanel
        loading={resource.loading && !group}
        error={!validId ? 'This group link is invalid.' : resource.error}
        onRetry={validId ? () => resource.reload() : undefined}
      />
      {group && (
        <>
          <section className="detail-panel">
            <div className="tag-list">
              <Badge tone={group.active ? 'good' : 'neutral'}>{group.active ? 'Open group' : 'Closed group'}</Badge>
              {group.viewer.leader ? (
                <Badge>Group leader</Badge>
              ) : group.viewer.member ? (
                <Badge tone="good">You are a member</Badge>
              ) : group.viewer.requestStatus === 'PENDING' ? (
                <Badge tone="pending">Application pending</Badge>
              ) : null}
            </div>
            <CapacityMeter members={group.memberCount} capacity={group.maxGroupSize} active={group.active} />
            <p className="message-text">{group.description || 'No description provided.'}</p>
            <dl className="detail-facts">
              <dt>Leader</dt>
              <dd>
                <Link to={`/students/${group.leaderId}`}>{group.leaderName}</Link>
              </dd>
              <dt>Study mode</dt>
              <dd>{label(group.preferredStudyMode)}</dd>
              <dt>Created</dt>
              <dd>{formatTimestamp(group.createdAt)} (SGT)</dd>
            </dl>
            <h3>Study goals</h3>
            <div className="tag-list">
              {group.studyGoals.length ?
                group.studyGoals.map(goal => (
                  <Badge key={goal}>{label(goal)}</Badge>
                )) : (
                  <p className="muted">No study goals selected.</p>
                )}
            </div>
            <div className="actions">
              <Button onClick={() => setAgenda(true)}>View agenda</Button>
              <CopyButton value={`${window.location.origin}/groups/${group.id}`} label="Copy group link" />
              {group.active && group.viewer.member && (
                <Link className="retro-button primary" to={`/groups/${group.id}/room`}>Open study room</Link>
              )}
              {group.viewer.leader && (
                <>
                  <Link className="retro-button primary" to={`/groups/${group.id}/manage`}>Manage group</Link>
                  {group.active && (
                    <Link className="retro-button" to={`/groups/${group.id}/edit`}>Edit group</Link>
                  )}
                </>
              )}
              {group.active && !group.viewer.member && group.viewer.requestStatus !== 'PENDING' &&
                group.memberCount < group.maxGroupSize && (
                  <Button
                    variant="primary"
                    onClick={() => setApplicationGroup({ id: group.id, name: group.name })}
                  >
                    Request membership
                  </Button>
                )}
            </div>
            {!group.active ? (
              <p className="muted">This group is closed. It cannot accept new members or changes.</p>
            ) : !group.viewer.member && group.viewer.requestStatus === 'PENDING' ? (
              <p className="muted">Your membership request is waiting for the leader's decision.</p>
            ) : !group.viewer.member && group.memberCount >= group.maxGroupSize ? (
              <p className="muted">This group is full. No membership requests can be sent until a place is available.</p>
            ) : !group.viewer.member && group.viewer.requestStatus === 'REJECTED' ? (
              <p className="muted">Your previous application was rejected. You may send a new request if a place is available.</p>
            ) : null}
          </section>
          <section className="detail-panel">
            <h2>Weekly meeting schedule</h2>
            <WeeklySchedule slots={group.availability} />
          </section>
          <section className="detail-panel">
            <h2>Accepted members</h2>
            <p className="privacy-note">Being in the same group does not share contact numbers. Open a profile to send a buddy request.</p>
            {!group.members.length ? (
              <p>No accepted members.</p>
            ) : (
              <div className="data-list">
                {group.members.map(member => (
                  <div className="data-row" key={member.studentId}>
                    <div>
                      <div className="person-heading"><Avatar name={member.name} small /><Link to={`/students/${member.studentId}`}>{member.name}</Link></div>
                      {member.leader && (
                        <> <Badge>Leader</Badge></>
                      )}
                      <p className="row-meta">Joined {formatTimestamp(member.joinedAt)} (SGT)</p>
                    </div>
                    <Link className="retro-button" to={`/students/${member.studentId}`}>View profile</Link>
                  </div>
                ))}
              </div>
            )}
          </section>
        </>
      )}
      {applicationGroup?.id === groupId &&
        <ApplyGroupDialog
          groupId={applicationGroup.id}
          groupName={applicationGroup.name}
          onClose={() => setApplicationGroup(null)}
        />}
      {agenda && group && (
        <Dialog title={`${group.name} agenda`} onClose={() => setAgenda(false)}>
          <h3>Study goals</h3>
          {group.studyGoals.length ? (
            <ul>{group.studyGoals.map(goal => (
              <li key={goal}>{label(goal)}</li>
            ))}</ul>
          ) : (
            <p>No study goals saved yet.</p>
          )}
          <h3>Weekly schedule</h3>
          <WeeklySchedule slots={group.availability} />
          <Button onClick={() => setAgenda(false)}>Close agenda</Button>
        </Dialog>
      )}
    </WindowPage>
  )
}
