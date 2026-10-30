import { api, refreshResources } from '../../shared/api/client'
export type NotificationFilter = 'ALL' | 'REQUESTS' | 'GROUPS'
export type Notification = {
  id: number
  type: string
  message: string
  read: boolean
  createdAt: string
  resourceType: 'MATCH_REQUEST' | 'GROUP' | 'STUDENT' | 'SESSION' | null
  resourceId: number | null
  eventKey: string | null
  requestDirection?: 'INCOMING' | 'OUTGOING' | null
}
export async function getNotifications(filter: NotificationFilter, signal?: AbortSignal) { return (await api.get<Notification[]>('/notifications', { params: { filter }, signal })).data }
export async function readNotification(id: number) { return (await api.post<Notification>(`/notifications/${id}/read`)).data }
export async function readAllNotifications() { await api.post('/notifications/read-all') }
/** Unread notifications across every filter, as the menu bar badge shows. */
export async function getUnreadCount(signal?: AbortSignal) { return (await api.get<{ count: number }>('/notifications/unread-count', { signal })).data.count }

/** Following a notification counts as reading it; failed writes leave it unread. */
export function followNotification(notification: Notification) {
  if (!notification.read) readNotification(notification.id).then(() => refreshResources(), () => undefined)
}

export function notificationLink(notification: Notification) {
  if (!notification.resourceId) return null
  if (notification.resourceType === 'GROUP') {
    if (notification.type === 'GROUP_STUDY_ROOM_STARTED') return { to: `/groups/${notification.resourceId}/room`, label: 'Open study room' }
    // A leader's join-request notice goes straight to where the decision is made
    return notification.type === 'GROUP_JOIN_REQUEST_RECEIVED'
      ? { to: `/groups/${notification.resourceId}/manage`, label: 'Review applications' }
      : { to: `/groups/${notification.resourceId}`, label: 'View group' }
  }
  if (notification.resourceType === 'SESSION') return { to: `/sessions/${notification.resourceId}`, label: 'View study session' }
  if (notification.resourceType === 'STUDENT') return { to: `/students/${notification.resourceId}`, label: 'View profile' }
  if (notification.resourceType === 'MATCH_REQUEST') {
    const direction = notification.requestDirection === undefined
      ? notification.type === 'MATCH_REQUEST_RECEIVED' ? 'INCOMING' : 'OUTGOING'
      : notification.requestDirection
    if (!direction) return null
    return {
      to: `/connections?view=${direction === 'INCOMING' ? 'incoming' : 'outgoing'}`,
      label: 'View buddy requests'
    }
  }
  return null
}
