import { useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { cancelSession, getSession } from './api'
import { displayedOccurrence, nextSessionOccurrence, occurrenceLabel, sessionParent, sessionStatusLabel } from './sessionPresentation'
import { useSessionClock } from './useSessionClock'
import SessionRoomLink from './SessionRoomLink'
import { useAuth } from '../../shared/auth/useAuth'
import { useResource } from '../../shared/api/useResource'
import { useAction } from '../../shared/api/useAction'
import WindowPage from '../../shared/components/WindowPage'
import StatePanel from '../../shared/components/StatePanel'
import ActionNotice from '../../shared/components/ActionNotice'
import Badge from '../../shared/components/Badge'
import Button from '../../shared/components/Button'
import Dialog from '../../shared/components/Dialog'
import CalendarButton from './CalendarButton'
import './sessions.css'

export default function SessionDetailPage() {
  const { id } = useParams()
  const { account } = useAuth()
  return <SessionDetail key={`${account?.id}-${id}`} id={Number(id)} />
}
function SessionDetail({ id }: { id: number }) {
  const { account } = useAuth()
  const valid = Number.isSafeInteger(id) && id > 0
  const session = useResource(`session-${account?.id}-${id}`, signal => getSession(id, signal), valid)
  const action = useAction()
  const [confirm, setConfirm] = useState(false)
  const data = session.data
  const clockSessions = useMemo(() => data ? [data] : [], [data])
  const now = useSessionClock(clockSessions)
  async function cancel() {
    if (!data) return
    const result = await action.run(() => cancelSession(id, data.version), 'Study session cancelled for everyone.')
    if (result.ok) setConfirm(false)
    else session.reload()
  }
  return <WindowPage title={data?.title ?? 'Study session'} description="A shared plan, with times displayed in the session’s timezone."
    actions={<Link className="retro-button" to={data ? sessionParent(data) : '/sessions'}>Back to sessions</Link>}>
    <ActionNotice error={action.error} success={action.success} />
    <StatePanel loading={session.loading && !data} error={valid ? session.error : 'This session link is invalid.'} onRetry={valid ? () => session.reload() : undefined} />
    {data && <>
      <section className="detail-panel">
        <div className="tag-list"><Badge tone={nextSessionOccurrence(data, now) ? 'good' : 'neutral'}>{sessionStatusLabel(data, now)}</Badge>
          <Badge>{data.scope === 'GROUP' ? 'Group session' : 'Buddy session'}</Badge></div>
        <h2>When and where</h2>
        <p>{occurrenceLabel(displayedOccurrence(data, now), data.timeZone)}</p>
        <p><strong>Timezone:</strong> {data.timeZone}</p>
        <p><strong>Location:</strong> {data.location || 'To be agreed'}</p>
        <h3>Agenda</h3><p className="message-text">{data.agenda || 'No agenda saved yet.'}</p>
        <div className="actions">
          {data.canManage && data.status === 'SCHEDULED' && <><Link className="retro-button primary" to={`/sessions/${id}/edit`}>Edit session</Link>
            <Button variant="danger" disabled={action.pending} onClick={() => setConfirm(true)}>Cancel session</Button></>}
          <SessionRoomLink session={data} now={now} />
          <CalendarButton sessionId={id} />
        </div>
        <p className="privacy-note">Calendar downloads are snapshots. Re-export after edits or cancellation, and check your calendar for duplicates. Each weekly date is exported as a separate event. No contacts or participant emails are included.</p>
        {data.status === 'CANCELLED' && <p className="muted">This plan is cancelled. Download the cancellation snapshot to update your calendar.</p>}
      </section>
      <section className="detail-panel"><h2>{data.occurrenceCount > 1 ? `${data.occurrenceCount} weekly dates` : 'Session date'}</h2>
        <ul className="session-times">{data.occurrences.map(item => <li key={item.startAt}>{occurrenceLabel(item, data.timeZone)}</li>)}</ul>
      </section>
    </>}
    {confirm && data?.canManage && <Dialog title="Cancel study session?" busy={action.pending} onClose={() => setConfirm(false)}>
      <p>This cancels {data.occurrenceCount > 1 ? `all ${data.occurrenceCount} weekly dates` : 'the session'} for everyone. Participants will be notified.</p>
      <ActionNotice error={action.error} />
      <div className="actions"><Button disabled={action.pending} onClick={() => setConfirm(false)}>Keep session</Button>
        <Button variant="danger" disabled={action.pending} onClick={() => void cancel()}>Cancel for everyone</Button></div>
    </Dialog>}
  </WindowPage>
}
