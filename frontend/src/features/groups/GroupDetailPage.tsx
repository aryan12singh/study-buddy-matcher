import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getGroup, leaveGroup, withdrawApplication } from './api'
import { useResource } from '../../shared/api/useResource'
import { useAction } from '../../shared/api/useAction'
import { label, formatTimestamp } from '../../shared/api/types'
import WindowPage from '../../shared/components/WindowPage'
import StatePanel from '../../shared/components/StatePanel'
import Badge from '../../shared/components/Badge'
import Button from '../../shared/components/Button'
import WeeklySchedule from '../../shared/components/WeeklySchedule'
import Dialog from '../../shared/components/Dialog'
import ConfirmDialog from '../../shared/components/ConfirmDialog'
import ActionNotice from '../../shared/components/ActionNotice'
import ApplyGroupDialog from './ApplyGroupDialog'
import PersonCard from '../../shared/components/PersonCard'
import CapacityMeter from '../../shared/components/CapacityMeter'
import CopyButton from '../../shared/components/CopyButton'
import GroupSessionAgenda from '../sessions/GroupSessionAgenda'
import { useAuth } from '../../shared/auth/useAuth'

export default function GroupDetailPage() {
  const { id } = useParams()
  const { account } = useAuth()
  return <GroupDetailContent key={`${account?.role}:${account?.id}:${id}`} actorId={account?.role === 'STUDENT' ? account.id : undefined} />
}

type MembershipConfirmation = {
  kind: 'leave' | 'withdraw'
  groupId: number
  groupName: string
  requestId: number | null
  joinedAt?: string
}

function GroupDetailContent({ actorId }: { actorId?: number }) {
  const { id } = useParams(),
    groupId = Number(id),
    validId = Number.isSafeInteger(groupId) && groupId > 0
  const resource = useResource(`group-${id}`, signal => getGroup(groupId, signal), validId && actorId !== undefined)
  const group = resource.data
  const [applicationGroup, setApplicationGroup] = useState<{ id: number; name: string } | null>(null)
  const [agenda, setAgenda] = useState(false)
  const [confirming, setConfirming] = useState<MembershipConfirmation | null>(null)
  const action = useAction()
  const joinedAt = group?.members.find(member => member.studentId === actorId)?.joinedAt
  const confirmationCurrent = Boolean(confirming && group?.id === confirming.groupId && group.active &&
    (confirming.kind === 'leave'
      ? group.viewer.member && !group.viewer.leader && joinedAt === confirming.joinedAt
      : !group.viewer.member && group.viewer.requestStatus === 'PENDING' && group.viewer.requestId === confirming.requestId))

  // Consent belongs to the exact record that was shown, including the current membership tenure.
  if (confirming && !confirmationCurrent) setConfirming(null)

  function beginMembershipAction(kind: MembershipConfirmation['kind']) {
    if (!group) return
    action.clear()
    setConfirming({ kind, groupId: group.id, groupName: group.name, requestId: group.viewer.requestId, joinedAt })
  }

  async function confirmMembershipAction() {
    if (!confirming || !confirmationCurrent) return
    const target = confirming
    const result = target.kind === 'leave'
      ? await action.run(() => leaveGroup(target.groupId), `You left ${target.groupName}.`, true)
      : await action.run(() => withdrawApplication(target.requestId!), 'Application withdrawn.')
    if (result.ok) setConfirming(null)
  }
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
      <ActionNotice error={confirming ? undefined : action.error} success={action.success} />
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
            {group.active && group.pendingApplications ? (
              <div className="attention-panel" role="status">
                <p>{group.pendingApplications === 1 ? '1 student has' : `${group.pendingApplications} students have`} asked to join this group.</p>
                <Link className="retro-button primary" to={`/groups/${group.id}/manage`}>Review applications</Link>
              </div>
            ) : null}
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
              {group.active && group.viewer.member && (
                <Link className="retro-button primary" to={`/groups/${group.id}/room`}>Open study room</Link>
              )}
              {group.viewer.leader && group.active && (
                <Link className="retro-button primary" to={`/groups/${group.id}/manage`}>Manage group</Link>
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
              {group.active && !group.viewer.member && group.viewer.requestStatus === 'PENDING' && group.viewer.requestId && (
                <Button disabled={action.pending} onClick={() => beginMembershipAction('withdraw')}>
                  Withdraw application
                </Button>
              )}
              {group.active && group.viewer.member && !group.viewer.leader && (
                <Button variant="danger" disabled={action.pending} onClick={() => beginMembershipAction('leave')}>
                  Leave group
                </Button>
              )}
              {/* Last, so its "Copied." confirmation appears at the end instead of pushing other buttons */}
              <CopyButton value={`${window.location.origin}/groups/${group.id}`} label="Copy group link" />
            </div>
            {!group.active ? (
              <p className="muted">This group is closed. It cannot accept new members or changes.</p>
            ) : !group.viewer.member && group.viewer.requestStatus === 'PENDING' ? (
              <p className="muted">Your membership request is waiting for the leader's decision.</p>
            ) : !group.viewer.member && group.memberCount >= group.maxGroupSize ? (
              <p className="muted">This group is full. No membership requests can be sent until a place is available.</p>
            ) : !group.viewer.member && group.viewer.requestStatus === 'REJECTED' ? (
              <p className="muted">Your previous application was rejected. You may send a new request if a place is available.</p>
            ) : !group.viewer.member && group.viewer.requestStatus === 'WITHDRAWN' ? (
              <p className="muted">You withdrew your previous application. You may send a new request if a place is available.</p>
            ) : null}
          </section>
          {group.active && group.viewer.member && <GroupSessionAgenda groupId={group.id} leader={group.viewer.leader} />}
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
                  <PersonCard key={member.studentId} badges={member.leader && <Badge>Leader</Badge>} studentId={member.studentId} name={member.name} compact>
                    <p className="row-meta">Joined {formatTimestamp(member.joinedAt)} (SGT)</p>
                  </PersonCard>
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
      {confirming && confirmationCurrent && (
        <ConfirmDialog
          title={confirming.kind === 'leave' ? `Leave ${confirming.groupName}?` : `Withdraw your application to ${confirming.groupName}?`}
          confirmLabel={confirming.kind === 'leave' ? 'Leave group' : 'Withdraw application'}
          pending={action.pending}
          error={action.error}
          onClose={() => setConfirming(null)}
          onConfirm={confirmMembershipAction}
        >
          <p>{confirming.kind === 'leave'
            ? 'You will no longer be a member and the leader will be told. To rejoin, you would need to apply again and be accepted.'
            : 'The leader will no longer see your application. You can apply again later while the group has space.'}</p>
        </ConfirmDialog>
      )}
      {agenda && group && (
        <Dialog title={`${group.name} agenda`} onClose={() => setAgenda(false)}>
          <div className="actions">{group.active && group.viewer.member && <Link className="retro-button primary" to={`/groups/${group.id}/sessions`}>View dated session agenda</Link>}</div>
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
