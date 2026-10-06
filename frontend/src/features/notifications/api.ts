import { api } from '../../shared/api/client'
export type NotificationFilter = 'ALL' | 'REQUESTS' | 'GROUPS'
export type Notification = {
  id: number
  type: string
  message: string
  read: boolean
  createdAt: string
  resourceType: 'MATCH_REQUEST' | 'GROUP' | 'STUDENT' | null
  resourceId: number | null
  eventKey: string | null
  requestDirection?: 'INCOMING' | 'OUTGOING' | null
}
export async function getNotifications(filter: NotificationFilter, signal?: AbortSignal) { return (await api.get<Notification[]>('/notifications', { params: { filter }, signal })).data }
export async function readNotification(id: number) { return (await api.post<Notification>(`/notifications/${id}/read`)).data }
export async function readAllNotifications() { await api.post('/notifications/read-all') }

export function notificationLink(notification: Notification) {
  if (!notification.resourceId) return null
  if (notification.resourceType === 'GROUP') return { to: `/groups/${notification.resourceId}`, label: 'View group' }
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
