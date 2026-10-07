import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getAccountSummary, getUsers, changeUserStatus } from './api'
import type { AdminAccountTarget } from './api'
import { useAuth } from '../../shared/auth/useAuth'
import { useResource } from '../../shared/api/useResource'
import { useAction } from '../../shared/api/useAction'
import { formatTimestamp, label } from '../../shared/api/types'
import WindowPage from '../../shared/components/WindowPage'
import StatePanel from '../../shared/components/StatePanel'
import Field from '../../shared/components/Field'
import Button from '../../shared/components/Button'
import Badge from '../../shared/components/Badge'
import ActionNotice from '../../shared/components/ActionNotice'
import DeactivateUserDialog from './DeactivateUserDialog'

export default function AdminUsersPage() {
  const { account } = useAuth(),
    action = useAction()
  const [role, setRole] = useState(''),
    [active, setActive] = useState(''),
    [searchInput, setSearchInput] = useState(''),
    [search, setSearch] = useState('')
  const [deactivating, setDeactivating] = useState<AdminAccountTarget | null>(null)
  useEffect(
    () => {
      const timer = window.setTimeout(() => setSearch(searchInput.trim()), 300)
      return () => window.clearTimeout(timer)
    },
    [searchInput]
  )
  const summary = useResource('admin-account-summary', getAccountSummary)
  const users = useResource(`admin-users-${role}-${active}-${search}`, signal => getUsers({ role, active, search }, signal))
  return (
    <WindowPage
      title="User accounts"
      description="Manage accounts and see their recorded study activity."
      actions={<Link className="retro-button primary" to="/admin/users/new">Create account</Link>}
    >
      <StatePanel
        loading={summary.loading && !summary.data}
        error={summary.error}
        onRetry={() => summary.reload()}
      />
      {summary.data && (
        <div className="stats-strip" aria-label="All account totals">
          {([
            { key: 'total', label: 'Total accounts' },
            { key: 'active', label: 'Active accounts' },
            { key: 'inactive', label: 'Inactive accounts' },
            { key: 'students', label: 'Students' },
            { key: 'admins', label: 'Administrators' }
          ] as const).map(stat => (
            <div key={stat.key} className="stat">
              <strong>{summary.data![stat.key]}</strong>
              <span>{stat.label}</span>
            </div>
          ))}
        </div>
      )}
      <div className="filters">
        <Field id="account-search" label="Search name or email">
          <input
            id="account-search"
            type="search"
            value={searchInput}
            maxLength={255}
            onChange={event => setSearchInput(event.target.value)}
          />
        </Field>
        <Field id="account-role" label="Role">
          <select
            id="account-role"
            value={role}
            onChange={event => setRole(event.target.value)}
          >
            <option value="">All roles</option>
            <option value="STUDENT">Student</option>
            <option value="ADMIN">Administrator</option>
          </select>
        </Field>
        <Field id="account-status" label="Account status">
          <select
            id="account-status"
            value={active}
            onChange={event => setActive(event.target.value)}
          >
            <option value="">All accounts</option>
            <option value="true">Active</option>
            <option value="false">Inactive</option>
          </select>
        </Field>
      </div>
      <ActionNotice error={action.error} success={action.success} />
      <StatePanel
        loading={users.loading && !users.data}
        error={users.error}
        onRetry={() => users.reload()}
        empty={users.data?.length === 0}
        emptyTitle="No accounts match"
        emptyMessage="Try another name, email or filter."
      />
      {users.data && users.data.length > 0 && (
        <div className="table-scroll">
          <table className="account-table">
            <caption className="row-meta">{users.data.length} account{users.data.length === 1 ? '' : 's'} in this view</caption>
            <thead>
              <tr>
                <th scope="col">Account</th>
                <th scope="col">Role</th>
                <th scope="col">Status</th>
                <th scope="col">Last successful login</th>
                <th scope="col">Actions</th>
              </tr>
            </thead>
            <tbody>
              {users.data.map(user => (
                <tr key={user.id}>
                  <td>
                    <Link to={`/admin/users/${user.id}`}>{user.name || user.email}</Link>
                    {user.name && (
                      <p className="row-meta">{user.email}</p>
                    )}
                  </td>
                  <td>{label(user.role)}</td>
                  <td>
                    <Badge tone={user.active ? 'good' : 'neutral'}>{user.active ? 'Active' : 'Inactive'}</Badge>
                  </td>
                  <td>
                    {formatTimestamp(user.lastLoginAt)}
                    {user.lastLoginAt && ' (SGT)'}
                  </td>
                  <td>
                    <div className="actions">
                      <Link className="retro-button" to={`/admin/users/${user.id}`}>View details</Link>
                      <Link className="retro-button" to={`/admin/users/${user.id}/edit`}>Edit account</Link>
                      {user.active ? (
                        <Button
                          variant="danger"
                          disabled={action.pending || user.id === account?.id}
                          onClick={() => setDeactivating({ id: user.id, email: user.email, role: user.role, name: user.name })}
                        >
                          Deactivate
                        </Button>
                      ) : (
                        <Button
                          disabled={action.pending}
                          onClick={() => action.run(() => changeUserStatus(user.id, 'reactivate'), 'Account reactivated. A fresh login is required.')}
                        >Reactivate</Button>
                      )}
                    </div>
                    {user.id === account?.id && (
                      <p className="row-meta">Your account cannot be deactivated or deleted.</p>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {deactivating && (
        <DeactivateUserDialog
          user={deactivating}
          available={!users.loading && Boolean(users.data?.some(user => user.id === deactivating.id && user.active))}
          onClose={() => setDeactivating(null)}
        />
      )}
    </WindowPage>
  )
}
