import { afterEach, describe, expect, it, vi } from 'vitest'
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { AppRoutes } from '../../App'
import { AuthContext } from '../../shared/auth/context'
import { api } from '../../shared/api/client'
import { renderPage, response, studentAccount } from '../../test/renderApp'
import { campusDate } from '../../shared/api/types'
import { notificationLink } from './api'
import NotificationsPage from './NotificationsPage'

const notification = { id: 30, type: 'MATCH_REQUEST_RECEIVED', message: 'Jamie sent a request.', read: false, createdAt: new Date().toISOString(), resourceType: 'MATCH_REQUEST' as const, resourceId: 10, eventKey: 'match-request:10:received' }
afterEach(() => vi.restoreAllMocks())
describe('notifications', () => {
  it('restores deep-linked categories with browser Back and ignores unknown categories', async () => {
    const get = vi.spyOn(api, 'get').mockResolvedValue(response([]))
    const { router } = renderPage(<NotificationsPage />, '/notifications?view=groups', '/notifications')
    await screen.findByText('No notifications here')
    expect(get).toHaveBeenCalledWith('/notifications', expect.objectContaining({ params: { filter: 'GROUPS' } }))
    expect(screen.getByRole('link', { name: 'Browse study groups' })).toHaveAttribute('href', '/groups')
    fireEvent.click(screen.getByRole('button', { name: 'Buddy requests' }))
    expect(router.state.location.search).toBe('?view=requests')
    await act(async () => { await router.navigate(-1) })
    expect(screen.getByRole('button', { name: 'Study groups' })).toHaveAttribute('aria-pressed', 'true')
    await act(async () => { await router.navigate('/notifications?view=unknown') })
    await waitFor(() => expect(get).toHaveBeenCalledWith('/notifications', expect.objectContaining({ params: { filter: 'ALL' } })))
  })
  it('groups by Singapore calendar day and uses safe action links', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response([notification, { ...notification, id: 31, message: 'A group closed.', createdAt: '2025-01-01T00:00:00Z', resourceType: null, resourceId: null, read: true }]))
    renderPage(<NotificationsPage />)
    expect(await screen.findByRole('heading', { name: 'Today' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Earlier' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'View buddy requests' })).toHaveAttribute('href', '/connections?view=incoming')
    expect(screen.getByText('This update has no available action.')).toBeInTheDocument()
    expect(campusDate('2026-10-06T18:00:00Z')).toEqual(campusDate('2026-10-07T02:00:00+08:00'))
  })
  it.each([
    { message: 'Jamie withdrew their request after account deactivation.', requestDirection: 'INCOMING' as const, view: 'incoming' },
    { message: 'Jamie declined your request.', requestDirection: 'OUTGOING' as const, view: 'outgoing' }
  ])('links a declined request notice to its $requestDirection history', async ({ message, requestDirection, view }) => {
    vi.spyOn(api, 'get').mockResolvedValue(response([{
      ...notification,
      type: 'MATCH_REQUEST_DECLINED',
      message,
      requestDirection
    }]))
    renderPage(<NotificationsPage />)
    expect(await screen.findByText(message)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'View buddy requests' })).toHaveAttribute('href', `/connections?view=${view}`)
  })
  it.each([
    { read: false, marks: true },
    { read: true, marks: false }
  ])('following the link of a notification with read=$read marks it read: $marks', async ({ read, marks }) => {
    vi.spyOn(api, 'get').mockResolvedValue(response([{ ...notification, read }]))
    const post = vi.spyOn(api, 'post').mockResolvedValue(response({ ...notification, read: true }))
    renderPage(<NotificationsPage />)
    fireEvent.click(await screen.findByRole('link', { name: 'View buddy requests' }))
    if (marks) expect(post).toHaveBeenCalledWith('/notifications/30/read')
    else expect(post).not.toHaveBeenCalled()
  })
  it('passes the selected category to the API and renders empty results', async () => {
    const get = vi.spyOn(api, 'get').mockResolvedValue(response([]))
    renderPage(<NotificationsPage />)
    await screen.findByText('No notifications here')
    fireEvent.click(screen.getByRole('button', { name: 'Study groups' }))
    await waitFor(() => expect(get).toHaveBeenLastCalledWith('/notifications', expect.objectContaining({ params: { filter: 'GROUPS' } })))
    expect(await screen.findByText('No notifications here')).toBeInTheDocument()
  })
  it('persists mark-one and mark-all actions then refetches read state', async () => {
    vi.spyOn(api, 'get').mockResolvedValueOnce(response([notification])).mockResolvedValue(response([{ ...notification, read: true }]))
    const post = vi.spyOn(api, 'post').mockResolvedValue(response({ ...notification, read: true }))
    renderPage(<NotificationsPage />)
    fireEvent.click(await screen.findByRole('button', { name: 'Mark as read' }))
    await waitFor(() => expect(screen.queryByText('Unread')).not.toBeInTheDocument())
    expect(post).toHaveBeenCalledWith('/notifications/30/read')
    fireEvent.click(screen.getByRole('button', { name: 'Mark all as read' }))
    await waitFor(() => expect(post).toHaveBeenCalledWith('/notifications/read-all'))
  })
  it('shows a retry and recovers from a failed read view', async () => {
    vi.spyOn(api, 'get').mockRejectedValueOnce(new Error('Network unavailable')).mockResolvedValue(response([]))
    renderPage(<NotificationsPage />)
    expect(await screen.findByRole('alert')).toHaveTextContent('Network unavailable')
    fireEvent.click(screen.getByRole('button', { name: 'Try again' }))
    expect(await screen.findByText('No notifications here')).toBeInTheDocument()
  })
  it('leaves deleted resources without a broken link', () => {
    expect(notificationLink({ ...notification, resourceId: null })).toBeNull()
    expect(notificationLink({ ...notification, requestDirection: null })).toBeNull()
    expect(notificationLink({ ...notification, type: 'MATCH_REQUEST_ACCEPTED' })?.to).toBe('/connections?view=outgoing')
    expect(notificationLink({ ...notification, resourceType: 'GROUP', resourceId: 5 })?.to).toBe('/groups/5')
    expect(notificationLink({ ...notification, type: 'GROUP_JOIN_REQUEST_RECEIVED', resourceType: 'GROUP', resourceId: 5 })).toEqual({ to: '/groups/5/manage', label: 'Review applications' })
  })
  it('refreshes the shell unread badge after a successful read mutation', async () => {
    let read = false
    vi.spyOn(api, 'get').mockImplementation(async path => response(path === '/notifications/unread-count' ? { count: read ? 0 : 1 } : [{ ...notification, read }]))
    vi.spyOn(api, 'post').mockImplementation(async () => { read = true; return response({ ...notification, read: true }) })
    render(<AuthContext.Provider value={{ account: studentAccount, loading: false, retry: vi.fn(), logout: vi.fn(), login: vi.fn(), register: vi.fn() }}><MemoryRouter initialEntries={['/notifications']}><AppRoutes /></MemoryRouter></AuthContext.Provider>)
    expect(await screen.findByLabelText('1 unread notifications')).toHaveTextContent('1')
    fireEvent.click(await screen.findByRole('button', { name: 'Mark as read' }))
    await waitFor(() => expect(screen.queryByLabelText('1 unread notifications')).not.toBeInTheDocument())
  })
})
