import { Link } from 'react-router-dom'
import Avatar from '../../shared/components/Avatar'
import PixelIcon from '../../shared/components/PixelIcon'
import StatePanel from '../../shared/components/StatePanel'
import type { MatchRequest } from '../connections/api'
import { timeAgo } from './homeTime'
import type { Loaded } from './homeTypes'
import SectionTitle from './SectionTitle'

const SHOWN = 3

/** A preview of pending requests; accepting and declining stay on the Connections page. */
export default function RequestCards({ requests, pending }: { requests: Loaded<MatchRequest[]>; pending: MatchRequest[] }) {
  return (
    <section aria-labelledby="home-requests">
      <SectionTitle
        id="home-requests"
        icon="connections"
        title="Waiting for your reply"
        count={pending.length ? String(pending.length) : undefined}
        link={pending.length ? { to: '/connections?view=incoming', label: 'All requests ›' } : undefined}
      />
      <StatePanel loading={requests.loading && !requests.data} error={requests.error} onRetry={() => requests.reload()} />
      {requests.data && (
        <div className="home-cards">
          {pending.length === 0 && (
            <div className="home-ghost home-ghost-full">
              <span className="home-ghost-icon"><PixelIcon kind="connections" /></span>
              <div><strong>No requests waiting</strong><p>When another student sends you a study buddy request, it appears here as a card you can review.</p></div>
            </div>
          )}
          {pending.slice(0, SHOWN).map(request => (
            <article key={request.id} className="home-request">
              <div className="home-request-top">
                <Avatar name={request.senderName} />
                <div>
                  <Link className="home-request-name" to={`/students/${request.senderId}`}>{request.senderName}</Link>
                  <small>{timeAgo(request.createdAt)}</small>
                </div>
              </div>
              <div className="home-tags">
                {request.context.courseCode && <span className="home-chip">{request.context.courseCode}</span>}
                <span className="home-via">{request.context.origin === 'MATCHING' ? 'From a matching search' : 'From their profile'}</span>
              </div>
              <p className={`home-request-message${request.message ? '' : ' none'}`}>
                {request.message ? `“${request.message}”` : 'No message added.'}
              </p>
              <div className="home-request-actions">
                <Link to={`/students/${request.senderId}`}>View profile</Link>
                <Link className="retro-button primary" to="/connections?view=incoming">Review request ›</Link>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  )
}
