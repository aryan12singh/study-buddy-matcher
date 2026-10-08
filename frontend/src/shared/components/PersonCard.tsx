import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import Avatar from './Avatar'

/**
 * One student in a list: name and "View profile" on the top line, details below,
 * and any decisions about that student (accept, approve, remove) on the bottom line.
 */
export default function PersonCard({ studentId, name, compact = false, children, actions }: {
  studentId: number
  name: string
  compact?: boolean
  children?: ReactNode
  actions?: ReactNode
}) {
  const profile = `/students/${studentId}`
  return (
    <article className="data-row person-card">
      <div className="person-heading">
        <Avatar name={name} small={compact} />
        <h2><Link to={profile}>{name}</Link></h2>
        <Link className="retro-button profile-link" to={profile}>View profile</Link>
      </div>
      {children}
      {actions && <div className="actions">{actions}</div>}
    </article>
  )
}
