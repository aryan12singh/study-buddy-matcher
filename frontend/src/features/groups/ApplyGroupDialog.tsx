import { useState } from 'react'
import { applyToGroup } from './api'
import { useAction } from '../../shared/api/useAction'
import Dialog from '../../shared/components/Dialog'
import Field from '../../shared/components/Field'
import Button from '../../shared/components/Button'
import ActionNotice from '../../shared/components/ActionNotice'

export default function ApplyGroupDialog({ groupId, groupName, onClose }: {
  groupId: number
  groupName: string
  onClose: () => void
}) {
  const [message, setMessage] = useState(''),
    [error, setError] = useState<string>(),
    action = useAction()

  async function submit(event: React.SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    if (message.trim().length > 255) {
      setError('Use up to 255 characters.')
      return
    }
    setError(undefined)
    const result = await action.run(() => applyToGroup(groupId, message), 'Group membership requested.')
    if (result.ok) onClose()
  }
  return (
    <Dialog
      title={`Request to join ${groupName}`}
      onClose={onClose}
      busy={action.pending}
    >
      <form
        className="stack-form"
        onSubmit={submit}
        noValidate
      >
        <p>The leader will review your application. Pending requests do not reserve a place.</p>
        <Field
          id="group-application-message"
          label="Message (optional)"
          error={error || action.errors.message}
        >
          <textarea
            id="group-application-message"
            maxLength={255}
            value={message}
            disabled={action.pending}
            onChange={event => setMessage(event.target.value)}
          />
        </Field>
        <ActionNotice error={action.error} />
        <div className="actions">
          <Button onClick={onClose} disabled={action.pending}>Cancel</Button>
          <Button
            type="submit"
            variant="primary"
            disabled={action.pending}
          >{action.pending ? 'Requesting…' : 'Request membership'}</Button>
        </div>
      </form>
    </Dialog>
  )
}
