import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { closeGroup, decideApplication, getApplicants, getGroup, removeMember } from './api'
import type { GroupApplication, GroupMember } from './api'
import { useResource } from '../../shared/api/useResource'
import { useAction } from '../../shared/api/useAction'
import { formatTimestamp } from '../../shared/api/types'
import WindowPage from '../../shared/components/WindowPage'
import StatePanel from '../../shared/components/StatePanel'
import TabBar from '../../shared/components/TabBar'
import Button from '../../shared/components/Button'
import Badge from '../../shared/components/Badge'
import ActionNotice from '../../shared/components/ActionNotice'
import ConfirmDialog from '../../shared/components/ConfirmDialog'
import PersonCard from '../../shared/components/PersonCard'
import CopyButton from '../../shared/components/CopyButton'
import CapacityMeter from '../../shared/components/CapacityMeter'
import { useAuth } from '../../shared/auth/useAuth'

export default function GroupManagePage() {
  const { id } = useParams()
  const { account } = useAuth()
  return <GroupManageContent key={`${account?.role}:${account?.id}:${id}`} enabled={account?.role === 'STUDENT'} />
}

function GroupManageContent({ enabled }: { enabled: boolean }) {
  const { id } = useParams(),
    groupId = Number(id),
    validId = Number.isSafeInteger(groupId) && groupId > 0
  const resource = useResource(`manage-group-${id}`, signal => getGroup(groupId, signal), validId && enabled)
  const group = resource.data
  const applicants = useResource(`group-applicants-${id}`, signal => getApplicants(groupId, signal), Boolean(group?.viewer.leader))
  const [tab, setTab] = useState<'applicants' | 'members'>('applicants'),
    [removing, setRemoving] = useState<GroupMember | null>(null),
    [closing, setClosing] = useState(false),
    [rejecting, setRejecting] = useState<GroupApplication | null>(null)
  const action = useAction()
  const confirmationCurrent = Boolean(group?.viewer.leader && group.active && (rejecting
    ? applicants.data?.some(application => application.id === rejecting.id && application.status === 'PENDING')
    : removing ? group.members.some(member => member.studentId === removing.studentId && member.joinedAt === removing.joinedAt)
      : closing))

  if ((removing || closing || rejecting) && !confirmationCurrent) {
    setRemoving(null)
    setClosing(false)
    setRejecting(null)
  }

  function dismissConfirmation() {
    setRemoving(null)
    setClosing(false)
    setRejecting(null)
  }

  async function confirm() {
    if (!confirmationCurrent) return
    const result = rejecting
      ? await action.run(() => decideApplication(groupId, rejecting.id, 'reject'), 'Application rejected.')
      : await action.run(
        async () => {
          if (removing) await removeMember(groupId, removing.studentId); else await closeGroup(groupId)
        },
        removing ? 'Member removed.' : 'Study group closed.',
        true
      )
    if (result.ok) dismissConfirmation()
  }
  return (
    <WindowPage
      title={group ? `Manage ${group.name}` : 'Manage study group'}
      description={group ?
        `${group.courseCode} · ${group.memberCount}/${group.maxGroupSize} accepted members, including the leader.`
        :
        undefined}
      actions={<Link className="retro-button" to={`/groups/${id}`}>Back to group</Link>}
    >
      <StatePanel
        loading={resource.loading && !group}
        error={!validId ? 'This group link is invalid.' : resource.error}
        onRetry={validId ? () => resource.reload() : undefined}
      />
      {group && !group.viewer.leader && <StatePanel error="Only this group’s leader can manage membership." />}
      {group?.viewer.leader && (
        <>
          <CapacityMeter members={group.memberCount} capacity={group.maxGroupSize} active={group.active} />
          <div className="actions page-actions">
            {group.active ? (
              <>
                <Link className="retro-button primary" to={`/groups/${group.id}/edit`}>Edit group</Link>
                <Button
                  variant="danger"
                  disabled={action.pending}
                  onClick={() => {
                    action.clear()
                    setClosing(true)
                  }}
                >
                  Close group
                </Button>
              </>
            ) : (
              <Badge>Closed group</Badge>
            )}
          </div>
          {!group.active && (
            <p className="muted">This group is closed. Membership decisions and edits are no longer available.</p>
          )}
          <ActionNotice error={removing || closing || rejecting ? undefined : action.error} success={action.success} />
          <TabBar
            value={tab}
            label="Group management views"
            options={[{ value: 'applicants', label: 'Applicants' }, { value: 'members', label: 'Members' }]}
            onChange={setTab}
          />
          {tab === 'applicants' ? (
            <>
              <StatePanel
                loading={applicants.loading && !applicants.data}
                error={applicants.error}
                onRetry={() => applicants.reload()}
                empty={applicants.data?.length === 0}
                emptyTitle="No pending applicants"
                emptyMessage="New requests to join this group will appear here."
                emptyKind="groups"
                emptyAction={group.active && <CopyButton label="Copy group link" value={`${window.location.origin}/groups/${group.id}`} />}
              />
              {group.memberCount >= group.maxGroupSize && group.active && (
                <p className="privacy-note">The group is full. Increase capacity or remove a member before approving another applicant.</p>
              )}
              <div className="data-list">
                {applicants.data?.map(application => (
                  <PersonCard
                    key={application.id}
                    studentId={application.studentId}
                    name={application.studentName}
                    actions={group.active && (
                      <>
                        <Button
                          variant="primary"
                          disabled={action.pending || group.memberCount >= group.maxGroupSize}
                          onClick={() => action.run(() => decideApplication(groupId, application.id, 'accept'), 'Application approved.')}
                        >
                          Approve application
                        </Button>
                        <Button
                          disabled={action.pending}
                          onClick={() => { action.clear(); setRejecting(application) }}
                        >Reject application</Button>
                      </>
                    )}
                  >
                    {application.message && (
                      <p className="message-text">{application.message}</p>
                    )}
                    <p className="row-meta">Requested {formatTimestamp(application.createdAt)} (SGT)</p>
                  </PersonCard>
                ))}
              </div>
            </>
          ) : (
            <>
              <StatePanel empty={group.members.length === 0} emptyTitle="No accepted members" />
              <div className="data-list">
                {group.members.map(member => (
                  <PersonCard
                    key={member.studentId}
                    studentId={member.studentId}
                    name={member.name}
                    actions={group.active && !member.leader && (
                      <Button
                        variant="danger"
                        disabled={action.pending}
                        onClick={() => {
                          action.clear()
                          setRemoving(member)
                        }}
                      >
                        Remove member
                      </Button>
                    )}
                  >
                    {member.leader && (
                      <Badge>Leader</Badge>
                    )}
                    <p className="row-meta">Joined {formatTimestamp(member.joinedAt)} (SGT)</p>
                  </PersonCard>
                ))}
              </div>
            </>
          )}
        </>
      )}
      {(removing || closing || rejecting) && confirmationCurrent && (
        <ConfirmDialog
          title={rejecting ? `Reject ${rejecting.studentName}'s application?` : removing ? `Remove ${removing.name}?` : 'Close this study group?'}
          confirmLabel={rejecting ? 'Reject application' : removing ? 'Remove member' : 'Close group'}
          pending={action.pending}
          error={action.error}
          onClose={dismissConfirmation}
          onConfirm={confirm}
        >
          {rejecting ? (
            <p>This answer is final and {rejecting.studentName} will be notified. They may apply again later while the group has space.</p>
          ) : removing ? (
            <p>This removes {removing.name} from the group and notifies them. Their buddy connections remain separate.</p>
          ) : (
            <>
              <p>Closing is permanent. New applications and member approvals will stop, and all pending applications will be rejected.</p>
              <p>The saved group and answered history remain available.</p>
            </>
          )}
        </ConfirmDialog>
      )}
    </WindowPage>
  )
}
