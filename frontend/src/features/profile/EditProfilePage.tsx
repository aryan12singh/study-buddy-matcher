import { Navigate, useParams } from 'react-router-dom'
import { useAuth } from '../../shared/auth/useAuth'
import OwnerProfilePage from './OwnerProfilePage'

/**
 * Edits the signed-in student's own profile at /students/:id/edit. Another
 * student's edit URL goes back to their read-only profile; the server only
 * ever edits the signed-in account, so this is navigation, not security.
 */
export default function EditProfilePage() {
  const { id } = useParams()
  const { account } = useAuth()
  if (account?.id !== Number(id)) return <Navigate to={`/students/${id}`} replace />
  // Changing or losing the signed-in owner unmounts all of this account's drafts.
  return <OwnerProfilePage key={account.id} id={account.id} />
}
