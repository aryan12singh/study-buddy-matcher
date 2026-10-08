import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from './useAuth'
import type { Role } from '../api/types'
import WindowPage from '../components/WindowPage'
import StatePanel from '../components/StatePanel'
import Button from '../components/Button'

export default function RequireRole({ role }: { role?: Role }) {
  const auth = useAuth()
  const location = useLocation()
  if (auth.loading || auth.error) return (
    <div className="desktop auth-desktop">
      <WindowPage title="Your session">
        <StatePanel
          loading={auth.loading}
          error={auth.error}
          onRetry={auth.retry}
        />
        {auth.error && (
          <Button onClick={auth.logout}>Return to login</Button>
        )}
      </WindowPage>
    </div>
  )
  if (!auth.account) return <Navigate
    to="/login"
    state={{ from: location.pathname + location.search }}
    replace
  />
  if (role && auth.account.role !== role) return <Navigate to={auth.account.role === 'ADMIN' ? '/admin/users' : '/connections'} replace />
  return <Outlet />
}
