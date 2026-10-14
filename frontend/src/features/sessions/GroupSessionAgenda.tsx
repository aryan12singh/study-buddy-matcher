import { Link } from 'react-router-dom'
import { getSessions } from './api'
import { useResource } from '../../shared/api/useResource'
import { useAuth } from '../../shared/auth/useAuth'
import StatePanel from '../../shared/components/StatePanel'
import SessionList from './SessionList'

export default function GroupSessionAgenda({ groupId, leader }: { groupId: number; leader: boolean }) {
  const { account } = useAuth()
  const sessions = useResource(`group-agenda-${account?.id}-${groupId}`, signal => getSessions({ kind: 'GROUP', id: groupId }, signal))
  const upcoming = sessions.data?.filter(session => session.status === 'SCHEDULED' && session.nextOccurrence)
    .sort((left, right) => Date.parse(left.nextOccurrence!.startAt) - Date.parse(right.nextOccurrence!.startAt))
    .slice(0, 3) ?? []
  return <section className="detail-panel" aria-labelledby="group-session-agenda">
    <h2 id="group-session-agenda">Dated session agenda</h2>
    <div className="actions"><Link className="retro-button" to={`/groups/${groupId}/sessions`}>All group sessions</Link>
      {leader && <Link className="retro-button primary" to={`/groups/${groupId}/sessions/new`}>Plan study session</Link>}</div>
    <StatePanel loading={sessions.loading && !sessions.data} error={sessions.error} onRetry={() => sessions.reload()} />
    {sessions.data && <SessionList sessions={upcoming} />}
  </section>
}
