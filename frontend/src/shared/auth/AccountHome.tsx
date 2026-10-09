import { Navigate } from 'react-router-dom'
import { useAuth } from './useAuth'

export default function AccountHome() {
  const { account } = useAuth()
  return <Navigate to={account?.role === 'ADMIN' ? '/admin/users' : '/home'} replace />
}
