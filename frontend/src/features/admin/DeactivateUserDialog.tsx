import { changeUserStatus } from './api'
import type { AdminAccountTarget } from './api'
import { useAction } from '../../shared/api/useAction'
import ConfirmDialog from '../../shared/components/ConfirmDialog'

export default function DeactivateUserDialog({ user, available, onClose, onDeactivated }: {
  user: AdminAccountTarget; available: boolean; onClose: () => void; onDeactivated: () => void
}) {
  const action = useAction()

  async function confirm() {
    if (!available) return
    const result = await action.run(() => changeUserStatus(user.id, 'deactivate'), undefined, true)
    if (result.ok) { onDeactivated(); onClose() }
  }
  return (
    <ConfirmDialog
      title={`Deactivate ${user.name || user.email}?`}
      confirmLabel="Deactivate account"
      pending={action.pending}
      error={action.error}
      confirmDisabled={!available}
      onConfirm={confirm}
      onClose={onClose}
    >
      <p>This blocks account access and invalidates existing sessions. The account and profile are retained.</p>
      {!available && <p role="status">Wait for the latest account status, or close this dialog to review the account.</p>}
      {user.role === 'STUDENT' && (
        <p>Active buddy connections will end, pending requests will be cancelled, led groups will close, and memberships in open groups and pending applications will be removed. Reactivation will not restore them.</p>
      )}
    </ConfirmDialog>
  )
}
