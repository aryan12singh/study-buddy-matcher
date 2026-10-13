import { afterEach, describe, expect, it, vi } from 'vitest'
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router-dom'
import { api } from '../../shared/api/client'
import { useResource } from '../../shared/api/useResource'
import type { Notification } from '../notifications/api'
import { deferred, response } from '../../test/renderApp'
import ActivityFeed from './ActivityFeed'

const notification: Notification = {
  id: 30, type: 'MATCH_REQUEST_RECEIVED', message: 'Jamie sent a request.', read: false,
  createdAt: new Date().toISOString(), resourceType: 'MATCH_REQUEST', resourceId: 10,
  eventKey: 'match-request:10:received', requestDirection: 'INCOMING'
}

/** Remains mounted across navigation, like the shell's unread count. */
function UnreadCount() {
  const resource = useResource('activity-follow-unread', async signal =>
    (await api.get<{ count: number }>('/notifications/unread-count', { signal })).data)
  return <p>{resource.data ? `${resource.data.count} unread notifications` : 'Loading unread count'}</p>
}

function renderActivity(item = notification) {
  const router = createMemoryRouter([
    { path: '/home', element: <ActivityFeed notifications={{ data: [item], loading: false, reload: vi.fn() }} /> },
    { path: '/connections', element: <h1>Buddy requests</h1> }
  ], { initialEntries: ['/home'] })
  render(<><UnreadCount /><RouterProvider router={router} /></>)
  return router
}

afterEach(() => vi.restoreAllMocks())

describe('following recent activity', () => {
  it('navigates immediately and refreshes the unread count only after the read succeeds', async () => {
    let read = false
    const get = vi.spyOn(api, 'get').mockImplementation(async () => response({ count: read ? 0 : 1 }))
    const pending = deferred<never>()
    const post = vi.spyOn(api, 'post').mockImplementation(() => pending.promise)
    const router = renderActivity()
    await screen.findByText('1 unread notifications')

    fireEvent.click(screen.getByRole('link', { name: 'View buddy requests ›' }))
    expect(await screen.findByRole('heading', { name: 'Buddy requests' })).toBeInTheDocument()
    expect(router.state.location.pathname + router.state.location.search).toBe('/connections?view=incoming')
    expect(post).toHaveBeenCalledExactlyOnceWith('/notifications/30/read')
    expect(screen.getByText('1 unread notifications')).toBeInTheDocument()
    expect(get).toHaveBeenCalledTimes(1)

    await act(async () => {
      read = true
      pending.resolve(response({ ...notification, read: true }))
    })
    await screen.findByText('0 unread notifications')
    expect(get).toHaveBeenCalledTimes(2)
  })

  it('does not write or refresh when an already read notification is followed', async () => {
    const get = vi.spyOn(api, 'get').mockResolvedValue(response({ count: 0 }))
    const post = vi.spyOn(api, 'post').mockResolvedValue(response({ ...notification, read: true }))
    const router = renderActivity({ ...notification, read: true })
    await screen.findByText('0 unread notifications')

    fireEvent.click(screen.getByRole('link', { name: 'View buddy requests ›' }))
    expect(await screen.findByRole('heading', { name: 'Buddy requests' })).toBeInTheDocument()
    expect(router.state.location.pathname + router.state.location.search).toBe('/connections?view=incoming')
    expect(post).not.toHaveBeenCalled()
    expect(get).toHaveBeenCalledTimes(1)
  })

  it('keeps navigation working and the unread count unchanged when marking read fails', async () => {
    const get = vi.spyOn(api, 'get').mockResolvedValue(response({ count: 1 }))
    const pending = deferred<never>()
    const post = vi.spyOn(api, 'post').mockImplementation(() => pending.promise)
    const router = renderActivity()
    await screen.findByText('1 unread notifications')

    fireEvent.click(screen.getByRole('link', { name: 'View buddy requests ›' }))
    expect(await screen.findByRole('heading', { name: 'Buddy requests' })).toBeInTheDocument()
    expect(post).toHaveBeenCalledExactlyOnceWith('/notifications/30/read')
    await act(async () => { pending.reject(new Error('Network unavailable')) })

    await waitFor(() => expect(screen.getByText('1 unread notifications')).toBeInTheDocument())
    expect(router.state.location.pathname + router.state.location.search).toBe('/connections?view=incoming')
    expect(get).toHaveBeenCalledTimes(1)
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })
})
