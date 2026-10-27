import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { getConnections, getMatchRequests, decideMatchRequest, endConnection, withdrawMatchRequest } from './api'
import type { Connection, MatchRequest } from './api'
import { useResource } from '../../shared/api/useResource'
import { useAction } from '../../shared/api/useAction'
import { formatTimestamp, label } from '../../shared/api/types'
import WindowPage from '../../shared/components/WindowPage'
import TabBar from '../../shared/components/TabBar'
import Button from '../../shared/components/Button'
import Badge from '../../shared/components/Badge'
import StatePanel from '../../shared/components/StatePanel'
import ActionNotice from '../../shared/components/ActionNotice'
import ConfirmDialog from '../../shared/components/ConfirmDialog'
import PersonCard from '../../shared/components/PersonCard'

/** What a request is about; a profile origin is the default, so only matching is called out. */
function contextSummary(context: { origin: string, courseCode?: string | null, studyGoal?: string | null }) {
  const topic = [context.courseCode, context.studyGoal && label(context.studyGoal)].filter(Boolean).join(' · ')
  return [context.origin === 'MATCHING' && 'Found through matching', topic && `For ${topic}`].filter(Boolean).join(' · ')
}

export default function ConnectionsPage() {
  const [search, setSearch] = useSearchParams()
  const requestedView = search.get('view')
  const tab = requestedView === 'outgoing' || requestedView === 'connected' ? requestedView : 'incoming'
  const [ending, setEnding] = useState<Connection | null>(null),
    [confirming, setConfirming] = useState<{ request: MatchRequest; kind: 'decline' | 'withdraw' } | null>(null)
  const action = useAction()
  const requests = useResource(
    `requests-${tab}`,
    signal => getMatchRequests(tab === 'outgoing' ? 'outgoing' : 'incoming', signal),
    tab === 'incoming' || tab === 'outgoing'
  )
  const connections = useResource('active-connections', getConnections, tab === 'connected')

  // Incoming is an inbox: answered requests move to a collapsed history; outgoing keeps every status visible.
  const waiting = tab === 'incoming' ? requests.data?.filter(request => request.status === 'PENDING') : requests.data
  const answered = tab === 'incoming' ? requests.data?.filter(request => request.status !== 'PENDING') ?? [] : []

  function requestRow(request: MatchRequest) {
    const id = tab === 'incoming' ? request.senderId : request.receiverId
    const name = tab === 'incoming' ? request.senderName : request.receiverName
    return (
      <PersonCard
        key={request.id}
        studentId={id}
        name={name}
        actions={request.status !== 'PENDING' ? undefined : tab === 'outgoing' ? (
          <Button
            disabled={action.pending}
            onClick={() => { action.clear(); setConfirming({ request, kind: 'withdraw' }) }}
          >Withdraw request</Button>
        ) : (
          <>
            <Button
              variant="primary"
              disabled={action.pending}
              onClick={() => action.run(() => decideMatchRequest(request.id, 'accept'), 'Request accepted.', true)}
            >
              Accept request
            </Button>
            <Button
              disabled={action.pending}
              onClick={() => { action.clear(); setConfirming({ request, kind: 'decline' }) }}
            >Decline request</Button>
          </>
        )}
      >
        <Badge
            tone={request.status === 'PENDING' ? 'pending' : request.status === 'ACCEPTED' ? 'good' : 'neutral'}
          >{label(request.status)}</Badge>
          {request.message && (
            <p className="message-text">{request.message}</p>
          )}
          <p className="row-meta">Sent {formatTimestamp(request.createdAt)}{request.respondedAt && ` · Answered ${formatTimestamp(request.respondedAt)}`}</p>
        {request.context && contextSummary(request.context) && (
          <p className="row-meta">{contextSummary(request.context)}</p>
        )}
      </PersonCard>
    )
  }

  async function confirmRequestAction() {
    if (!confirming) return
    const { request, kind } = confirming
    const result = kind === 'decline'
      ? await action.run(() => decideMatchRequest(request.id, 'decline'), 'Request declined.')
      : await action.run(() => withdrawMatchRequest(request.id), 'Request withdrawn.')
    if (result.ok) setConfirming(null)
  }

  async function disconnect() {
    if (!ending) return
    const result = await action.run(() => endConnection(ending.id), 'Connection ended. Contact access has been removed.', true)
    if (result.ok) setEnding(null)
  }
  return (
    <WindowPage
      title="Connections"
      description="Manage buddy requests and accepted study connections."
      actions={<Link className="retro-button primary" to="/find-buddies">Find study buddies</Link>}
    >
      <TabBar
        label="Connection views"
        value={tab}
        options={[
          { value: 'incoming', label: 'Incoming requests' },
          { value: 'outgoing', label: 'Outgoing requests' },
          { value: 'connected', label: 'Connected buddies' }
        ]}
        onChange={value => {
          setSearch({ view: value })
          action.clear()
        }}
      />
      <ActionNotice error={ending || confirming ? undefined : action.error} success={action.success} />
      {tab === 'connected' ? (
        <>
          <StatePanel
            loading={connections.loading && !connections.data}
            error={connections.error}
            onRetry={() => connections.reload()}
            empty={connections.data?.length === 0}
            emptyTitle="No connected buddies yet"
            emptyMessage="Send a match request or accept an incoming request to connect."
            emptyKind="connections"
            emptyAction={<Link className="retro-button" to="/find-buddies">Find study buddies</Link>}
          />
          {connections.data?.length === 0 && !connections.error && (
            <p className="privacy-note"><Link to="/help#help-privacy">How requests and contact privacy work</Link></p>
          )}
          <div className="data-list">
            {connections.data?.map(connection => (
              <PersonCard
                key={connection.id}
                studentId={connection.otherStudentId}
                name={connection.otherStudentName}
                actions={
                  <>
                  <Link className="retro-button" to={`/connections/${connection.id}/sessions`}>Plan study sessions</Link>
                  <Button
                    variant="danger"
                    disabled={action.pending}
                    onClick={() => {
                      action.clear()
                      setEnding(connection)
                    }}
                  >
                    Disconnect
                  </Button>
                  </>
                }
              >
                <p className="row-meta">Connected since {formatTimestamp(connection.createdAt)}</p>
                <Badge tone="good">Connected</Badge>
              </PersonCard>
            ))}
          </div>
        </>
      ) : (
        <>
          <StatePanel
            loading={requests.loading && !requests.data}
            error={requests.error}
            onRetry={() => requests.reload()}
            empty={waiting?.length === 0}
            emptyTitle={tab === 'incoming' ? (answered.length > 0 ? 'No requests waiting for you' : 'No incoming requests') : 'No outgoing requests'}
            emptyMessage={tab === 'incoming' ?
              (answered.length > 0 ? 'Requests you have answered are under Past requests below.' : 'Requests from other students will appear here.')
              :
              'Your sent requests and their decisions will appear here.'}
            emptyKind="connections"
            emptyAction={<Link className="retro-button" to="/find-buddies">Find study buddies</Link>}
          />
          {waiting?.length === 0 && !requests.error && (
            <p className="privacy-note"><Link to="/help#help-privacy">How requests and contact privacy work</Link></p>
          )}
          <div className="data-list">
            {waiting?.map(requestRow)}
          </div>
          {answered.length > 0 && (
            <details className="notification-section">
              <summary>Past requests ({answered.length})</summary>
              <div className="data-list">
                {answered.map(requestRow)}
              </div>
            </details>
          )}
        </>
      )}
      {confirming && (
        <ConfirmDialog
          title={confirming.kind === 'decline'
            ? `Decline ${confirming.request.senderName}'s request?`
            : `Withdraw your request to ${confirming.request.receiverName}?`}
          confirmLabel={confirming.kind === 'decline' ? 'Decline request' : 'Withdraw request'}
          pending={action.pending}
          error={action.error}
          onClose={() => setConfirming(null)}
          onConfirm={confirmRequestAction}
        >
          <p>{confirming.kind === 'decline'
            ? 'This answer is final. They can send you a new request later if you both want to study together.'
            : 'The recipient will see this in Past requests and can no longer accept it. You can send a new request later.'}</p>
        </ConfirmDialog>
      )}
      {ending && (
        <ConfirmDialog
          title={`Disconnect from ${ending.otherStudentName}?`}
          confirmLabel="Disconnect"
          pending={action.pending}
          error={action.error}
          onClose={() => setEnding(null)}
          onConfirm={disconnect}
        >
          <p>This ends your study connection. Future profile views will no longer share either student's contact number.</p>
        </ConfirmDialog>
      )}
    </WindowPage>
  )
}
