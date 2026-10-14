import { Link, useParams } from 'react-router-dom'
import { getMySessions, getSessions } from './api'
import { scopeListPath } from './api'
import { getGroup } from '../groups/api'
import { getConnections } from '../connections/api'
import { useAuth } from '../../shared/auth/useAuth'
import { useResource } from '../../shared/api/useResource'
import WindowPage from '../../shared/components/WindowPage'
import StatePanel from '../../shared/components/StatePanel'
import SessionList from './SessionList'
import './sessions.css'

export default function SessionsPage({ kind }: { kind?: 'GROUP' | 'BUDDY' }) {
  const { id } = useParams()
  const { account } = useAuth()
  return <SessionsView key={`${account?.id}-${kind}-${id}`} kind={kind} id={Number(id)} />
}
function SessionsView({ kind, id }: { kind?: 'GROUP' | 'BUDDY'; id: number }) {
  const { account } = useAuth()
  const valid = !kind || Number.isSafeInteger(id) && id > 0
  const scope = kind ? { kind, id } : undefined
  const sessions = useResource(`sessions-${account?.id}-${kind}-${id}`, signal => scope ? getSessions(scope, signal) : getMySessions(signal), valid)
  const context = useResource(`session-context-${account?.id}-${kind}-${id}`, async signal => {
    if (kind === 'GROUP') {
      const group = await getGroup(id, signal)
      return { name: group.name, canCreate: group.active && group.viewer.leader, canRead: group.active && group.viewer.member, parent: `/groups/${id}` }
    }
    const connection = (await getConnections(signal)).find(item => item.id === id)
    return { name: connection ? `Sessions with ${connection.otherStudentName}` : 'Buddy sessions', canCreate: !!connection, canRead: !!connection, parent: '/connections?view=connected' }
  }, !!kind && valid)
  const denied = !!kind && !!context.data && !context.data.canRead
  return <WindowPage title={kind ? context.data?.name ?? 'Study sessions' : 'Your upcoming sessions'}
    description="Dated plans for study groups and connected buddies. Attendance is agreed between participants."
    actions={kind ? <Link className="retro-button" to={context.data?.parent ?? '/sessions'}>Back</Link> : <Link className="retro-button" to="/groups?view=mine">My groups</Link>}>
    <StatePanel loading={sessions.loading && !sessions.data || !!kind && context.loading && !context.data}
      error={!valid ? 'This session list link is invalid.' : denied ? 'Current group membership or an active buddy connection is required.' : sessions.error || context.error}
      onRetry={valid && !denied ? () => { sessions.reload(); if (kind) context.reload() } : undefined} />
    {valid && !denied && (!kind || context.data) && sessions.data && <>
      {context.data?.canCreate && scope && <div className="actions"><Link className="retro-button primary" to={`${scopeListPath(scope)}/new`}>Plan study session</Link></div>}
      {kind === 'GROUP' && !context.data?.canCreate && <p className="muted">The group leader plans and manages sessions.</p>}
      <SessionList sessions={sessions.data} />
      {!kind && <p className="muted">Plan sessions from a group’s agenda or an active connection.</p>}
    </>}
  </WindowPage>
}
