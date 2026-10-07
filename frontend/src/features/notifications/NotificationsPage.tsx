import { Link, useSearchParams } from 'react-router-dom'
import { getNotifications, notificationLink, readAllNotifications, readNotification } from './api'
import type { NotificationFilter } from './api'
import { useResource } from '../../shared/api/useResource'
import { useAction } from '../../shared/api/useAction'
import { campusDate, formatTimestamp } from '../../shared/api/types'
import WindowPage from '../../shared/components/WindowPage'
import Button from '../../shared/components/Button'
import TabBar from '../../shared/components/TabBar'
import StatePanel from '../../shared/components/StatePanel'
import ActionNotice from '../../shared/components/ActionNotice'
import Badge from '../../shared/components/Badge'
import PixelIcon from '../../shared/components/PixelIcon'

export default function NotificationsPage() {
  const [search, setSearch] = useSearchParams()
  const filter: NotificationFilter = search.get('view') === 'requests' ? 'REQUESTS' : search.get('view') === 'groups' ? 'GROUPS' : 'ALL'
  const action = useAction()
  const notifications = useResource(`notifications-${filter}`, signal => getNotifications(filter, signal))
  const today = campusDate(new Date().toISOString())
  return (
    <WindowPage
      title="Notifications"
      description="Buddy requests, group updates and account changes. Times shown in Singapore time."
      actions={<Button
        disabled={action.pending}
        onClick={() => action.run(readAllNotifications, 'All notifications marked as read.')}
      >Mark all as read</Button>}
    >
      <TabBar
        value={filter}
        label="Notification filters"
        options={[
          { value: 'ALL', label: 'All notifications' },
          { value: 'REQUESTS', label: 'Buddy requests' },
          { value: 'GROUPS', label: 'Study groups' }
        ]}
        onChange={value => {
          setSearch(previous => {
            const next = new URLSearchParams(previous)
            if (value === 'ALL') next.delete('view')
            else next.set('view', value.toLowerCase())
            return next
          })
          action.clear()
        }}
      />
      <ActionNotice error={action.error} success={action.success} />
      {notifications.data && <p className="row-meta">{notifications.data.length} notification{notifications.data.length === 1 ? '' : 's'} · {notifications.data.filter(item => !item.read).length} unread in this view</p>}
      <StatePanel
        loading={notifications.loading && !notifications.data}
        error={notifications.error}
        onRetry={() => notifications.reload()}
        empty={notifications.data?.length === 0}
        emptyTitle="No notifications here"
        emptyMessage="Updates matching this filter will appear when something changes."
        emptyKind="notifications"
        emptyAction={<Link className="retro-button" to={filter === 'GROUPS' ? '/groups' : '/connections'}>{filter === 'GROUPS' ? 'Browse study groups' : 'Open connections'}</Link>}
      />
      {['Today', 'Earlier'].map(section => {
        const items = notifications.data?.filter(item => (campusDate(item.createdAt) === today) === (section === 'Today')) || []
        return items.length > 0 && (
          <section key={section} className="notification-section">
            <h2>{section}</h2>
            <div className="data-list">
              {items.map(notification => {
                const link = notificationLink(notification)
                return (
                  <article
                    key={notification.id}
                    className={`data-row ${notification.read ? '' : 'notification-unread'}`}
                  >
                    <div>
                      <div className="notification-symbol"><PixelIcon kind={notification.type.startsWith('GROUP') ? 'groups' : 'connections'} /></div>
                      {!notification.read && (
                        <Badge tone="pending">Unread</Badge>
                      )}
                      <p className="message-text">{notification.message}</p>
                      <p className="row-meta">{formatTimestamp(notification.createdAt)} (SGT)</p>
                      {!link && (
                        <p className="row-meta">This update has no available action.</p>
                      )}
                    </div>
                    <div className="actions">
                      {link && (
                        <Link className="retro-button" to={link.to}>{link.label}</Link>
                      )}
                      {!notification.read && (
                        <Button
                          disabled={action.pending}
                          onClick={() => action.run(() => readNotification(notification.id), 'Notification marked as read.')}
                        >Mark as read</Button>
                      )}
                    </div>
                  </article>
                )
              })}
            </div>
          </section>
        )
      })}
    </WindowPage>
  )
}
