import { Link } from 'react-router-dom'
import type { MyProfile } from '../profile/api'
import { setupProgress } from './nextStep'

export default function ProfileStatusBadge({ profile, studentId }: { profile?: MyProfile; studentId: number }) {
  if (!profile) return null
  const progress = setupProgress(profile)
  if (progress.done < progress.total) {
    return <span className="home-status home-status-todo">Profile {progress.done} of {progress.total} steps</span>
  }
  return (
    <span className="home-status">
      <span aria-hidden="true">✓</span>Profile complete
      <Link className="home-status-link" to={`/students/${studentId}/edit`} aria-label="Edit my profile">Edit ›</Link>
    </span>
  )
}
