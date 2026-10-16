import { Link } from 'react-router-dom'
import { getMySessions } from './api'
import { useResource } from '../../shared/api/useResource'
import { useAuth } from '../../shared/auth/useAuth'
import StatePanel from '../../shared/components/StatePanel'
import SessionList from './SessionList'
import './sessions.css'

export default function UpcomingSessions() {
  const { account } = useAuth()
  const sessions = useResource(`home-sessions-${account?.id}`, getMySessions)
  return <section className="home-box session-preview" aria-labelledby="home-sessions">
    <div className="home-box-head"><h2 id="home-sessions">Upcoming study sessions</h2><Link className="home-more" to="/sessions">All upcoming sessions ›</Link></div>
    <StatePanel loading={sessions.loading && !sessions.data} error={sessions.error} onRetry={() => sessions.reload()} />
    {sessions.data && <SessionList sessions={sessions.data} upcomingOnly limit={3} />}
  </section>
}
