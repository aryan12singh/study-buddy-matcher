import { Link } from 'react-router-dom'
import { useResource } from '../../shared/api/useResource'
import PixelIcon from '../../shared/components/PixelIcon'
import type { PixelIconKind } from '../../shared/components/PixelIcon'
import StatePanel from '../../shared/components/StatePanel'
import { getStudentSummary } from '../students/api'

/** Four linked counts; a zero is greyed out so it does not read like data. */
export default function ActivityStrip() {
  const summary = useResource('home-summary', getStudentSummary)
  if (!summary.data) {
    return <StatePanel loading={summary.loading} error={summary.error} onRetry={() => summary.reload()} />
  }
  const stats: { value: number; label: string; to: string; icon: PixelIconKind }[] = [
    { value: summary.data.pendingIncoming, label: 'Requests waiting for you', to: '/connections?view=incoming', icon: 'connections' },
    { value: summary.data.activeConnections, label: 'Active connections', to: '/connections', icon: 'profile' },
    { value: summary.data.pendingOutgoing, label: 'Requests you sent', to: '/connections?view=outgoing', icon: 'send' },
    { value: summary.data.acceptedGroups, label: 'Study groups', to: '/groups?view=mine', icon: 'groups' }
  ]
  return (
    <nav className="home-tiles" aria-label="Your activity">
      {stats.map(stat => (
        <Link key={stat.label} to={stat.to} aria-label={`${stat.value} ${stat.label}`}
          className={`home-tile${stat.value === 0 ? ' home-tile-zero' : ''}`}>
          <span className="home-tile-icon"><PixelIcon kind={stat.icon} /></span>
          <span className="home-tile-text"><strong>{stat.value}</strong><span>{stat.label}</span></span>
          <span className="home-tile-go" aria-hidden="true">›</span>
        </Link>
      ))}
    </nav>
  )
}
