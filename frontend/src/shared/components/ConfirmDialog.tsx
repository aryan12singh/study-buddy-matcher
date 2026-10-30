import type { ReactNode } from 'react'
import Dialog from './Dialog'
import Button from './Button'
import ActionNotice from './ActionNotice'

export default function ConfirmDialog({ title, children, confirmLabel, cancelLabel = 'Cancel', onConfirm, onClose, pending, error, confirmDisabled = false }: {
  title: string
  children: ReactNode
  confirmLabel: string
  /** Names the safe choice when "Cancel" would be ambiguous, such as "Keep session" beside "Cancel for everyone". */
  cancelLabel?: string
  onConfirm: () => void
  onClose: () => void
  pending: boolean
  error?: string
  confirmDisabled?: boolean
}) {
  return (
    <Dialog
      title={title}
      onClose={onClose}
      busy={pending}
    >
      <div className="dialog-copy">{children}</div>
      <ActionNotice error={error} />
      <div className="actions dialog-actions">
        <Button onClick={onClose} disabled={pending}>{cancelLabel}</Button>
        <Button
          variant="danger"
          onClick={onConfirm}
          disabled={pending || confirmDisabled}
        >{pending ? 'Working…' : confirmLabel}</Button>
      </div>
    </Dialog>
  )
}
