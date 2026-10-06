import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { closeGroup, decideApplication, getApplicants, getGroup, removeMember } from './api'
import type { GroupMember } from './api'
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

export default function GroupManagePage() {
  const { id } = useParams(),
    groupId = Number(id),
    validId = Number.isSafeInteger(groupId) && groupId > 0
  const resource = useResource(`manage-group-${id}`, signal => getGroup(groupId, signal), validId)
  const group = resource.data
  const applicants = useResource(`group-applicants-${id}`, signal => getApplicants(groupId, signal), Boolean(group?.viewer.leader))
  const [tab, setTab] = useState<'applicants' | 'members'>('applicants'),
    [removing, setRemoving] = useState<GroupMember | null>(null),
    [closing, setClosing] = useState(false)
  const action = useAction()

  async function confirm() {
    const result = await action.run(
      async () => {
        if (removing) await removeMember(groupId, removing.studentId); else await closeGroup(groupId)
      },
      removing ? 'Member removed.' : 'Study group closed.',
      true
    )
    if (result.ok) {
      setRemoving(null)
      setClosing(false)
    }
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
          <div className="actions">
            {group.active ? (
              <>
                <Link className="retro-button" to={`/groups/${group.id}/edit`}>Edit group</Link>
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
          <ActionNotice error={removing || closing ? undefined : action.error} success={action.success} />
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
              />
              {group.memberCount >= group.maxGroupSize && group.active && (
                <p className="privacy-note">The group is full. Increase capacity or remove a member before approving another applicant.</p>
              )}
              <div className="data-list">
                {applicants.data?.map(application => (
                  <article className="data-row" key={application.id}>
                    <div>
                      <h2>
                        <Link to={`/students/${application.studentId}`}>{application.studentName}</Link>
                      </h2>
                      {application.message && (
                        <p className="message-text">{application.message}</p>
                      )}
                      <p className="row-meta">Requested {formatTimestamp(application.createdAt)} (SGT)</p>
                    </div>
                    <div className="actions">
                      <Link className="retro-button" to={`/students/${application.studentId}`}>View profile</Link>
                      {group.active && (
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
                            onClick={() => action.run(() => decideApplication(groupId, application.id, 'reject'), 'Application rejected.')}
                          >Reject application</Button>
                        </>
                      )}
                    </div>
                  </article>
                ))}
              </div>
            </>
          ) : (
            <>
              <StatePanel empty={group.members.length === 0} emptyTitle="No accepted members" />
              <div className="data-list">
                {group.members.map(member => (
                  <article className="data-row" key={member.studentId}>
                    <div>
                      <h2>
                        <Link to={`/students/${member.studentId}`}>{member.name}</Link>
                      </h2>
                      {member.leader && (
                        <Badge>Leader</Badge>
                      )}
                      <p className="row-meta">Joined {formatTimestamp(member.joinedAt)} (SGT)</p>
                    </div>
                    <div className="actions">
                      <Link className="retro-button" to={`/students/${member.studentId}`}>View profile</Link>
                      {group.active && !member.leader && (
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
                    </div>
                  </article>
                ))}
              </div>
            </>
          )}
        </>
      )}
      {(removing || closing) && (
        <ConfirmDialog
          title={removing ? `Remove ${removing.name}?` : 'Close this study group?'}
          confirmLabel={removing ? 'Remove member' : 'Close group'}
          pending={action.pending}
          error={action.error}
          onClose={() => {
            setRemoving(null)
            setClosing(false)
          }}
          onConfirm={confirm}
        >
          {removing ? (
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
