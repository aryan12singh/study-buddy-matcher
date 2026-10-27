import { describe, expect, it } from 'vitest'
import { notificationLink } from './api'
import type { Notification } from './api'

const notice = (type: string): Notification => ({ id: 1, type, message: 'Revision crew: the study room has started.', read: false,
  createdAt: '2026-10-24T02:00:00Z', resourceType: 'GROUP', resourceId: 5, eventKey: null })

describe('notification links', () => {
  it('opens the group study room from a room-started invitation', () => {
    expect(notificationLink(notice('GROUP_STUDY_ROOM_STARTED'))).toEqual({ to: '/groups/5/room', label: 'Open study room' })
  })

  it('keeps other group notices on the group page', () => {
    expect(notificationLink(notice('GROUP_MEMBER_LEFT'))).toEqual({ to: '/groups/5', label: 'View group' })
  })
})
