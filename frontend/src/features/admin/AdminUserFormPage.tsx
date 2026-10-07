import { useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { createUser, getUser, updateUser } from './api'
import { useResource } from '../../shared/api/useResource'
import { useAction } from '../../shared/api/useAction'
import { useViewNavigation } from '../../shared/components/useViewNavigation'
import type { Role } from '../../shared/api/types'
import { label } from '../../shared/api/types'
import { validateAccountFields } from '../../shared/auth/validation'
import WindowPage from '../../shared/components/WindowPage'
import StatePanel from '../../shared/components/StatePanel'
import Field from '../../shared/components/Field'
import AccountFields from '../../shared/components/AccountFields'
import Button from '../../shared/components/Button'
import ActionNotice from '../../shared/components/ActionNotice'

export default function AdminUserFormPage() {
  const { id } = useParams(),
    userId = Number(id),
    validId = !id || Number.isSafeInteger(userId) && userId > 0
  const resource = useResource(`admin-edit-${id}`, signal => getUser(userId, signal), Boolean(id) && validId, false)
  const navigate = useViewNavigation(),
    action = useAction(),
    initialised = useRef<string | null>(null)
  const [form, setForm] = useState({
    email: '',
    password: '',
    name: '',
    school: '',
    programme: '',
    yearOfStudy: '1',
    contactNumber: ''
  })
  const [role, setRole] = useState<Role>('STUDENT'),
    [errors, setErrors] = useState<Record<string, string>>({})
  useEffect(
    () => {
      if (resource.data && initialised.current !== id) {
        const { account, profile } = resource.data
        setRole(account.role)
        setForm({
          email: account.email,
          password: '',
          name: profile?.name || '',
          school: profile?.school || '',
          programme: profile?.programme || '',
          yearOfStudy: String(profile?.yearOfStudy || 1),
          contactNumber: ''
        })
        initialised.current = id || null
      }
    },
    [resource.data, id]
  )

  async function submit(event: React.SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    const invalid = validateAccountFields(form, role === 'STUDENT', !id)
    setErrors(invalid)
    if (Object.keys(invalid).length) return
    const input = {
      email: form.email.trim().toLowerCase(),
      ...(role === 'STUDENT' ?
        {
          name: form.name.trim(),
          school: form.school.trim(),
          programme: form.programme.trim(),
          yearOfStudy: Number(form.yearOfStudy),
          ...(form.contactNumber.trim() ? { contactNumber: form.contactNumber.trim() } : {})
        }
        :
        {})
    }
    const result = await action.run(
      () => id ? updateUser(userId, input) : createUser({ ...input, role, password: form.password }),
      id ? 'Account updated.' : 'Account created.',
      true
    )
    if (result.ok) navigate(`/admin/users/${result.value.account.id}`)
  }
  return (
    <WindowPage
      title={id ? 'Edit account' : 'Create account'}
      description={id ?
        'Role stays fixed. A blank contact replacement preserves the saved number.'
        :
        'Create a student or administrator account with a new password.'}
      actions={<Link className="retro-button" to={id ? `/admin/users/${id}` : '/admin/users'}>Cancel</Link>}
    >
      <StatePanel
        loading={resource.loading && !resource.data}
        error={!validId ? 'This account link is invalid.' : resource.error}
        onRetry={id && validId ? () => resource.reload() : undefined}
      />
      {validId && (!id || resource.data) && (
        <form
          className="stack-form"
          onSubmit={submit}
          noValidate
        >
          {id ? (
            <p>Account role: <strong>{label(role)}</strong></p>
          ) : (
            <Field id="new-account-role" label="Account role">
              <select
                id="new-account-role"
                value={role}
                disabled={action.pending}
                onChange={event => setRole(event.target.value as Role)}
              >
                <option value="STUDENT">Student</option>
                <option value="ADMIN">Administrator</option>
              </select>
            </Field>
          )}
          <AccountFields
            value={form}
            onChange={setForm}
            errors={{ ...action.errors, ...errors }}
            student={role === 'STUDENT'}
            creating={!id}
            disabled={action.pending}
          />
          {role === 'STUDENT' && (
            <p className="privacy-note">Contact numbers are accepted as input and withheld from all admin responses.</p>
          )}
          <ActionNotice error={action.error} />
          <div className="actions">
            <Link className="retro-button" to={id ? `/admin/users/${id}` : '/admin/users'}>Cancel</Link>
            <Button
              type="submit"
              variant="primary"
              disabled={action.pending}
            >{action.pending ? 'Saving…' : id ? 'Save account changes' : 'Create account'}</Button>
          </div>
        </form>
      )}
    </WindowPage>
  )
}
