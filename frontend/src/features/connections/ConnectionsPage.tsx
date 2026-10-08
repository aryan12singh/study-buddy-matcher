import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { getConnections, getMatchRequests, decideMatchRequest, endConnection } from './api'
import type { Connection } from './api'
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
import Avatar from '../../shared/components/Avatar'

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
              <article className="data-row" key={connection.id}>
                <div>
                  <div className="person-heading"><Avatar name={connection.otherStudentName} /><h2>
                    <Link to={`/students/${connection.otherStudentId}`}>{connection.otherStudentName}</Link>
                  </h2></div>
                  <p className="row-meta">Connected since {formatTimestamp(connection.createdAt)} (SGT)</p>
                  <Badge tone="good">Connected</Badge>
                </div>
                <div className="actions">
                  <Link className="retro-button" to={`/students/${connection.otherStudentId}`}>View profile</Link>
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
                </div>
              </article>
            ))}
          </div>
        </>
      ) : (
        <>
          <StatePanel
            loading={requests.loading && !requests.data}
            error={requests.error}
            onRetry={() => requests.reload()}
            empty={requests.data?.length === 0}
            emptyTitle={tab === 'incoming' ? 'No incoming requests' : 'No outgoing requests'}
            emptyMessage={tab === 'incoming' ?
              'Requests from other students will appear here.'
              :
              'Your sent requests and their decisions will appear here.'}
            emptyKind="connections"
            emptyAction={<Button onClick={() => { action.clear(); setNotice(undefined); setSending(true) }}>Start a buddy request</Button>}
          />
          <div className="data-list">
            {requests.data?.map(request => {
              const id = tab === 'incoming' ? request.senderId : request.receiverId
              const name = tab === 'incoming' ? request.senderName : request.receiverName
              return (
                <article className="data-row" key={request.id}>
                  <div>
                    <div className="person-heading"><Avatar name={name} /><h2>
                      <Link to={`/students/${id}`}>{name}</Link>
                    </h2></div>
                    <Badge
                      tone={request.status === 'PENDING' ? 'pending' : request.status === 'ACCEPTED' ? 'good' : 'neutral'}
                    >{label(request.status)}</Badge>
                    {request.message && (
                      <p className="message-text">{request.message}</p>
                    )}
                    <p className="row-meta">Sent {formatTimestamp(request.createdAt)} (SGT){request.respondedAt && ` · Answered ${formatTimestamp(request.respondedAt)}`}</p>
                    {request.context && (
                      <p className="row-meta">From {request.context.origin === 'MATCHING' ? 'matching' : 'a profile'}{request.context.courseCode && ` · ${request.context.courseCode}`}{request.context.studyGoal && ` · ${label(request.context.studyGoal)}`}</p>
                    )}
                  </div>
                  <div className="actions">
                    <Link className="retro-button" to={`/students/${id}`}>View profile</Link>
                    {tab === 'incoming' && request.status === 'PENDING' && (
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
                  </div>
                </article>
              )
            })}
          </div>
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
