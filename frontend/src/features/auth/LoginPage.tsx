import { useState } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../shared/auth/useAuth'
import { useAction } from '../../shared/api/useAction'
import WindowPage from '../../shared/components/WindowPage'
import Field from '../../shared/components/Field'
import Button from '../../shared/components/Button'
import ActionNotice from '../../shared/components/ActionNotice'

function LoginPage() {
  const auth = useAuth(),
    action = useAction(),
    navigate = useNavigate(),
    location = useLocation()
  const [email, setEmail] = useState(''),
    [password, setPassword] = useState('')
  const [errors, setErrors] = useState<Record<string, string>>({})
  if (auth.account) return <Navigate to="/app" replace />

  async function submit(event: React.SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    const invalid: Record<string, string> = {}
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) invalid.email = 'Enter a valid email address.'
    if (!password) invalid.password = 'Enter your password.'
    setErrors(invalid)
    if (Object.keys(invalid).length) return
    const result = await action.run(() => auth.login(email, password))
    if (result.ok) {
      const from = location.state?.from
      navigate(typeof from === 'string' && from.startsWith('/') && !from.startsWith('//') ? from : '/app', { replace: true })
    }
  }
  return (
    <div className="desktop auth-desktop">
      <WindowPage title="Log in" description="Your study buddies and groups are waiting.">
        <form
          onSubmit={submit}
          noValidate
          className="stack-form"
        >
          <Field
            id="login-email"
            label="Email address"
            error={errors.email || action.errors.email}
          >
            <input
              id="login-email"
              type="email"
              autoComplete="username"
              value={email}
              onChange={event => setEmail(event.target.value)}
              required
              aria-invalid={Boolean(errors.email || action.errors.email)}
            />
          </Field>
          <Field
            id="login-password"
            label="Password"
            error={errors.password || action.errors.password}
          >
            <input
              id="login-password"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={event => setPassword(event.target.value)}
              required
              aria-invalid={Boolean(errors.password || action.errors.password)}
            />
          </Field>
          <ActionNotice error={action.error} />
          <Button
            type="submit"
            variant="primary"
            disabled={action.pending || auth.loading}
          >{action.pending ? 'Logging in…' : 'Log in'}</Button>
          <p>New here? <Link to="/register">Create a student account</Link></p>
          <Link to="/">Back to home</Link>
        </form>
      </WindowPage>
    </div>
  )
}
export default LoginPage
