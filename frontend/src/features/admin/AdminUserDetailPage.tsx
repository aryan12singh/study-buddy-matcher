import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { changeUserStatus, deleteUser, getUser } from './api'
import type { AdminAccountTarget } from './api'
import { useAuth } from '../../shared/auth/useAuth'
import { useResource } from '../../shared/api/useResource'
import { useAction } from '../../shared/api/useAction'
import { useViewNavigation } from '../../shared/components/useViewNavigation'
import { formatTimestamp, label } from '../../shared/api/types'
import WindowPage from '../../shared/components/WindowPage'
import StatePanel from '../../shared/components/StatePanel'
import Button from '../../shared/components/Button'
import Badge from '../../shared/components/Badge'
import Field from '../../shared/components/Field'
import ActionNotice from '../../shared/components/ActionNotice'
import ConfirmDialog from '../../shared/components/ConfirmDialog'
import DeactivateUserDialog from './DeactivateUserDialog'

export default function AdminUserDetailPage() {
  const { id } = useParams(),
    userId = Number(id),
    validId = Number.isSafeInteger(userId) && userId > 0
  const auth = useAuth(),
    navigate = useViewNavigation(),
    action = useAction()
  const resource = useResource(`admin-user-${id}`, signal => getUser(userId, signal), validId)
  const user = resource.data?.account,
    profile = resource.data?.profile,
    usage = resource.data?.usage
  const [deactivating, setDeactivating] = useState<AdminAccountTarget | null>(null),
    [deleting, setDeleting] = useState<AdminAccountTarget | null>(null),
    [confirmation, setConfirmation] = useState('')
  const deleteReady = Boolean(deleting && deleting.id === userId && !resource.loading
    && user?.id === deleting.id && user.email === deleting.email)

  async function permanentDelete() {
    if (!deleting || !deleteReady || confirmation !== deleting.email) return
    const result = await action.run(() => deleteUser(deleting.id), 'Account permanently deleted.', true)
    if (result.ok) navigate('/admin/users')
  }
  return (
    <WindowPage
      title={user?.name || user?.email || 'Account details'}
      description="Identity, account status and recorded usage."
      actions={<Link className="retro-button" to="/admin/users">Back to accounts</Link>}
    >
      <StatePanel
        loading={resource.loading && !user}
        error={!validId ? 'This account link is invalid.' : resource.error}
        onRetry={validId ? () => resource.reload() : undefined}
      />
      <ActionNotice error={deleting ? undefined : action.error} success={action.success} />
      {user && (
        <>
          <section className="detail-panel">
            <div className="tag-list">
              <Badge>{label(user.role)}</Badge>
              <Badge tone={user.active ? 'good' : 'neutral'}>{user.active ? 'Active' : 'Inactive'}</Badge>
            </div>
            <dl className="detail-facts">
              <dt>Email</dt>
              <dd>{user.email}</dd>
              <dt>Created</dt>
              <dd>{formatTimestamp(user.createdAt)} (SGT)</dd>
              <dt>Last successful login</dt>
              <dd>
                {formatTimestamp(user.lastLoginAt)}
                {user.lastLoginAt && ' (SGT)'}
              </dd>
              {profile && (
                <>
                  <dt>Name</dt>
                  <dd>{profile.name}</dd>
                  <dt>School</dt>
                  <dd>{profile.school}</dd>
                  <dt>Programme</dt>
                  <dd>{profile.programme}</dd>
                  <dt>Year of study</dt>
                  <dd>{profile.yearOfStudy}</dd>
                </>
              )}
            </dl>
            <div className="actions">
              <Link className="retro-button" to={`/admin/users/${user.id}/edit`}>Edit account</Link>
              {user.active ? (
                <Button
                  variant="danger"
                  disabled={action.pending || user.id === auth.account?.id}
                  onClick={() => setDeactivating({ id: user.id, email: user.email, role: user.role, name: user.name })}
                >
                  Deactivate account
                </Button>
              ) : (
                <Button
                  disabled={action.pending}
                  onClick={() => action.run(() => changeUserStatus(user.id, 'reactivate'), 'Account reactivated. A fresh login is required.')}
                >Reactivate account</Button>
              )}
            </div>
            {user.id === auth.account?.id && (
              <p className="muted">You cannot deactivate or delete your own account.</p>
            )}
          </section>
          <section className="detail-panel">
            <h2>Basic usage</h2>
            {usage ? (
              <>
                <dl className="detail-facts">
                  <dt>Active connections</dt>
                  <dd>{usage.activeConnections}</dd>
                  <dt>Accepted groups</dt>
                  <dd>{usage.acceptedGroups}</dd>
                  <dt>Buddy requests sent</dt>
                  <dd>{usage.matchRequestsSent}</dd>
                  <dt>Groups led</dt>
                  <dd>{usage.groupsLed}</dd>
                  <dt>Other groups joined</dt>
                  <dd>{usage.groupsJoined}</dd>
                </dl>
                <p className="field-hint">Accepted groups counts current membership in open groups, including leadership. Buddy requests and led-group counts include history; other groups joined excludes groups led.</p>
              </>
            ) : (
              <p>Administrator accounts do not participate in matching or study groups.</p>
            )}
          </section>
          <section className="detail-panel">
            <h2>Delete permanently</h2>
            <p>Permanent deletion removes the account, profile and dependent records. This cannot be undone.</p>
            <Button
              variant="danger"
              disabled={action.pending || user.id === auth.account?.id}
              onClick={() => {
                action.clear()
                setConfirmation('')
                setDeleting({ id: user.id, email: user.email, role: user.role, name: user.name })
              }}
            >
              Delete account permanently
            </Button>
          </section>
        </>
      )}
      {deactivating && deactivating.id === userId && (
        <DeactivateUserDialog
          user={deactivating}
          available={!resource.loading && user?.id === deactivating.id && user.active}
          onClose={() => setDeactivating(null)}
        />
      )}
      {deleting && deleting.id === userId && (
        <ConfirmDialog
          title="Delete this account permanently?"
          confirmLabel="Delete permanently"
          pending={action.pending}
          error={action.error}
          confirmDisabled={!deleteReady || confirmation !== deleting.email}
          onClose={() => setDeleting(null)}
          onConfirm={permanentDelete}
        >
          <p>This removes {deleting.email} and all of their account data. They will lose access immediately.</p>
          {!deleteReady && <p role="status">Wait for the latest account details. If they have changed, close this dialog and review them before deleting.</p>}
          {deleting.role === 'STUDENT' && (
            <ul>
              <li>The student profile, availability, courses and study goals</li>
              <li>Buddy requests and connections</li>
              <li>Memberships, group applications and recipient notifications</li>
              <li>Every group they lead and those groups' dependent records</li>
            </ul>
          )}
          <p>Other groups remain. Affected users are notified; old resource links are removed safely.</p>
          <Field id="delete-account-confirmation" label={`Type ${deleting.email} to confirm`}>
            <input
              id="delete-account-confirmation"
              type="text"
              value={confirmation}
              disabled={action.pending}
              onChange={event => setConfirmation(event.target.value)}
              autoComplete="off"
            />
          </Field>
        </ConfirmDialog>
      )}
    </WindowPage>
  )
}
