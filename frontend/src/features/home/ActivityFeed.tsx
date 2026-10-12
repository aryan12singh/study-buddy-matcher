import { Fragment } from 'react'
import { Link } from 'react-router-dom'
import PixelIcon from '../../shared/components/PixelIcon'
import type { PixelIconKind } from '../../shared/components/PixelIcon'
import StatePanel from '../../shared/components/StatePanel'
import { notificationLink } from '../notifications/api'
import type { Notification } from '../notifications/api'
import { feedTime, isToday } from './homeTime'
import type { Loaded } from './homeTypes'
import SectionTitle from './SectionTitle'

const SHOWN = 5

function kind(type: string): { css: string; icon: PixelIconKind } {
  if (type.startsWith('GROUP')) return { css: 'group', icon: 'groups' }
  if (type.startsWith('CONNECTION') || type === 'MATCH_REQUEST_ACCEPTED') return { css: 'connection', icon: 'profile' }
  return { css: 'request', icon: 'connections' }
}

export default function ActivityFeed({ notifications }: { notifications: Loaded<Notification[]> }) {
  const recent = notifications.data?.slice(0, SHOWN) ?? []
  const unread = recent.filter(item => !item.read).length
  const groups = [
    { title: 'Today', items: recent.filter(item => isToday(item.createdAt)) },
    { title: 'Earlier', items: recent.filter(item => !isToday(item.createdAt)) }
  ].filter(group => group.items.length)
  return (
    <section aria-labelledby="home-activity">
      <SectionTitle
        id="home-activity"
        icon="notifications"
        title="Recent activity"
        count={unread ? `${unread} new` : undefined}
        link={recent.length ? { to: '/notifications', label: 'All notifications ›' } : undefined}
      />
      <StatePanel loading={notifications.loading && !notifications.data} error={notifications.error} onRetry={() => notifications.reload()} />
      {notifications.data && (
        <div className="home-box">
          {recent.length === 0 ? (
            <div className="home-empty-row">
              <span className="home-ghost-icon"><PixelIcon kind="notifications" /></span>
              <div><strong>You are all caught up</strong><span>Requests, connections and group updates will show up here.</span></div>
            </div>
          ) : (
            <ul className="home-feed">
              {groups.map(group => (
                <Fragment key={group.title}>
                  <li className="home-feed-day">{group.title}</li>
                  {group.items.map(item => {
                    const type = kind(item.type), link = notificationLink(item)
                    return (
                      <li key={item.id} className={`home-feed-item${item.read ? '' : ' new'}`}>
                        <span className={`home-feed-type ${type.css}`}><PixelIcon kind={type.icon} /></span>
                        <p>{item.message}{!item.read && <span className="home-new-pill">New</span>}</p>
                        <span className="home-feed-time">{feedTime(item.createdAt)}</span>
                        {link ? <Link to={link.to}>{link.label} ›</Link> : <span />}
                      </li>
                    )
                  })}
                </Fragment>
              ))}
            </ul>
          )}
        </div>
      )}
    </section>
  )
}
