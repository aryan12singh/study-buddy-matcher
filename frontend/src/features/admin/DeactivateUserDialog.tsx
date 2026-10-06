import { changeUserStatus } from './api'
import type { AdminUser } from './api'
import { useAction } from '../../shared/api/useAction'
import ConfirmDialog from '../../shared/components/ConfirmDialog'

export default function DeactivateUserDialog({ user, onClose }: { user: AdminUser; onClose: () => void }) {
  const action = useAction()

  async function confirm() {
    const result = await action.run(() => changeUserStatus(user.id, 'deactivate'), 'Account deactivated.', true)
    if (result.ok) onClose()
  }
  return (
    <ConfirmDialog
      title={`Deactivate ${user.name || user.email}?`}
      confirmLabel="Deactivate account"
      pending={action.pending}
      error={action.error}
      onConfirm={confirm}
      onClose={onClose}
    >
      <p>This blocks account access and invalidates existing sessions. The account and profile are retained.</p>
      {user.role === 'STUDENT' && (
        <p>Active buddy connections will end, pending requests will be declined, led groups will close, and memberships and pending applications will be removed. Reactivation will not restore them.</p>
      )}
    </ConfirmDialog>
  )
}
