import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../../shared/auth/useAuth'
import { useAction } from '../../shared/api/useAction'
import { validateAccountFields } from '../../shared/auth/validation'
import WindowPage from '../../shared/components/WindowPage'
import AccountFields from '../../shared/components/AccountFields'
import Button from '../../shared/components/Button'
import ActionNotice from '../../shared/components/ActionNotice'

function RegisterPage() {
  const auth = useAuth(),
    action = useAction(),
    navigate = useNavigate()
  const [form, setForm] = useState({
    email: '',
    password: '',
    name: '',
    school: '',
    programme: '',
    yearOfStudy: '1',
    contactNumber: ''
  })
  const [errors, setErrors] = useState<Record<string, string>>({})
  if (auth.account) return <Navigate to="/app" replace />

  async function submit(event: React.SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    const invalid = validateAccountFields(form, true, true)
    setErrors(invalid)
    if (Object.keys(invalid).length) return
    const result = await action.run(() => auth.register({
      ...form,
      name: form.name.trim(),
      school: form.school.trim(),
      programme: form.programme.trim(),
      contactNumber: form.contactNumber.trim(),
      yearOfStudy: Number(form.yearOfStudy)
    }))
    if (result.ok) navigate(`/students/${result.value.id}/edit?welcome=1&tab=preferences`, { replace: true, state: { notice: 'Account created.' } })
  }
  return (
    <div className="desktop auth-desktop">
      <WindowPage
        title="Create a student account"
        description="Start with your identity. You will add courses and study preferences right after."
      >
        <form
          onSubmit={submit}
          noValidate
          className="stack-form"
        >
          <AccountFields
            value={form}
            onChange={setForm}
            errors={{ ...action.errors, ...errors }}
            student
            creating
            disabled={action.pending}
          />
          <p className="privacy-note">Your contact number is shared only with accepted study buddies.</p>
          <ActionNotice error={action.error} />
          <Button
            variant="primary"
            type="submit"
            disabled={action.pending}
          >{action.pending ? 'Creating account…' : 'Create student account'}</Button>
          <p>Already registered? <Link to="/login">Log in</Link></p>
        </form>
      </WindowPage>
    </div>
  )
}
export default RegisterPage
