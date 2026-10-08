import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { getConnections, getMatchRequests, decideMatchRequest, endConnection } from './api'
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
import SendRequestDialog from './SendRequestDialog'
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
  const [sending, setSending] = useState(false),
    [ending, setEnding] = useState<Connection | null>(null)
  const [notice, setNotice] = useState<string>()
  const action = useAction()
  const requests = useResource(
    `requests-${tab}`,
    signal => getMatchRequests(tab === 'outgoing' ? 'outgoing' : 'incoming', signal),
    tab !== 'connected'
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
        actions={tab === 'incoming' && request.status === 'PENDING' && (
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
              onClick={() => action.run(() => decideMatchRequest(request.id, 'decline'), 'Request declined.')}
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
          <p className="row-meta">Sent {formatTimestamp(request.createdAt)} (SGT){request.respondedAt && ` · Answered ${formatTimestamp(request.respondedAt)}`}</p>
        {request.context && contextSummary(request.context) && (
          <p className="row-meta">{contextSummary(request.context)}</p>
        )}
      </PersonCard>
    )
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
      actions={<Button variant="primary" onClick={() => {
        action.clear()
        setNotice(undefined)
        setSending(true)
      }}>
        Send match request
      </Button>}
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
          setNotice(undefined)
          action.clear()
        }}
      />
      <ActionNotice error={ending ? undefined : action.error} success={notice || action.success} />
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
            emptyAction={<Button onClick={() => { action.clear(); setNotice(undefined); setSending(true) }}>Start a buddy request</Button>}
          />
          <div className="data-list">
            {connections.data?.map(connection => (
              <PersonCard
                key={connection.id}
                studentId={connection.otherStudentId}
                name={connection.otherStudentName}
                actions={
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
                }
              >
                <p className="row-meta">Connected since {formatTimestamp(connection.createdAt)} (SGT)</p>
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
            emptyAction={<Button onClick={() => { action.clear(); setNotice(undefined); setSending(true) }}>Start a buddy request</Button>}
          />
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
      {sending &&
        <SendRequestDialog onClose={() => setSending(false)} onSent={() => {
          setSearch({ view: 'outgoing' })
          setNotice('Match request sent.')
        }} />}
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
