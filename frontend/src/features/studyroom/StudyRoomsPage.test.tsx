import { afterEach, describe, expect, it, vi } from 'vitest'
import { act, fireEvent, render, screen, within } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router-dom'
import { appRoutes } from '../../routes'
import { api } from '../../shared/api/client'
import { AuthContext } from '../../shared/auth/context'
import type { Account } from '../../shared/auth/context'
import { deferred, renderPage, response, studentAccount } from '../../test/renderApp'
import type { GroupSummary } from '../groups/api'
import StudyRoomsPage from './StudyRoomsPage'

const group: GroupSummary = {
  id: 5, name: 'Revision crew', courseId: 3, courseCode: 'IS442', courseName: 'Object Oriented Programming',
  leaderId: 1, leaderName: 'Priya', preferredStudyMode: null, studyGoals: [], maxGroupSize: 4, memberCount: 2,
  active: true, viewer: { leader: true, member: true, requestId: null, requestStatus: null },
}

function openApplication(path: string, account: Account | null = studentAccount) {
  const router = createMemoryRouter(appRoutes, { initialEntries: [path] })
  const auth = { account, loading: false, login: vi.fn(), register: vi.fn(), logout: vi.fn(), retry: vi.fn() }
  render(<AuthContext.Provider value={auth}><RouterProvider router={router} /></AuthContext.Provider>)
  return router
}

afterEach(() => { vi.restoreAllMocks(); sessionStorage.clear() })

describe('Study Room navigation', () => {
  it('opens a personal timer alongside all memberships without joining a shared room automatically', async () => {
    vi.spyOn(api, 'get').mockImplementation(async path => response(path === '/notifications/unread-count' ? { count: 0 } : [
      group,
      { ...group, id: 6, name: 'Member crew', viewer: { ...group.viewer, leader: false } },
      { ...group, id: 7, name: 'Closed crew', active: false },
    ]))
    const post = vi.spyOn(api, 'post')
    const router = openApplication('/groups?view=mine')
    const navigation = within(screen.getByRole('navigation', { name: 'Main navigation' }))
    fireEvent.click(navigation.getByRole('link', { name: 'Study Room' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Study Room' })).toBeInTheDocument()
    expect(await screen.findByRole('link', { name: 'Open study room for Revision crew' })).toHaveAttribute('href', '/groups/5/room')
    expect(screen.getByRole('link', { name: 'Open study room for Member crew' })).toHaveAttribute('href', '/groups/6/room')
    expect(screen.getByRole('link', { name: 'View closed group Closed crew' })).toHaveAttribute('href', '/groups/7')
    expect(screen.queryByRole('link', { name: 'Open study room for Closed crew' })).not.toBeInTheDocument()
    expect(screen.getByRole('complementary', { name: 'Your study groups' })).toBeInTheDocument()
    expect(screen.getByRole('timer')).toHaveTextContent('25:00')
    expect(router.state.location.pathname).toBe('/study-room')
    expect(navigation.getByRole('link', { name: 'Study Room' })).toHaveAttribute('aria-current', 'page')
    expect(navigation.getByRole('link', { name: 'Study groups' })).not.toHaveAttribute('aria-current', 'page')
    expect(post).not.toHaveBeenCalled()
  })

  it('shows loading, handles a failed request and retries to an empty state with a way to find groups', async () => {
    const pending = deferred<never>()
    let groupRequests = 0
    vi.spyOn(api, 'get').mockImplementation(path => path === '/weather' ? Promise.resolve(response({ available: false }))
      : groupRequests++ === 0 ? pending.promise : Promise.resolve(response([])))
    renderPage(<StudyRoomsPage />)
    expect(within(screen.getByRole('complementary', { name: 'Your study groups' })).getByRole('status')).toHaveTextContent('Loading')
    await act(async () => { pending.reject(new Error('Unable to load your groups.')) })
    expect(await screen.findByRole('alert')).toHaveTextContent('Unable to load your groups.')
    fireEvent.click(screen.getByRole('button', { name: 'Try again' }))
    expect(await screen.findByText('Your company is on its way')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Browse study groups' })).toHaveAttribute('href', '/groups')
    expect(screen.getByRole('button', { name: 'Start focus' })).toBeEnabled()
  })

  it('hides the personal panel and expands to full screen with the timer alone', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response([group]))
    renderPage(<StudyRoomsPage />)
    const workspace = document.querySelector('.study-room-workspace')
    fireEvent.click(await screen.findByRole('button', { name: 'Hide panel' }))
    expect(workspace).toHaveClass('is-minimal')
    expect(screen.getByRole('timer')).toHaveTextContent('25:00')
    // The panel's own controls are hidden by CSS, which this test environment does not apply.
    const view = within(screen.getByRole('group', { name: 'Timer view' }))
    fireEvent.click(view.getByRole('button', { name: 'Full screen' }))
    expect(workspace).toHaveClass('is-expanded')
    fireEvent.click(view.getByRole('button', { name: 'Exit full screen' }))
    expect(workspace).toHaveClass('is-minimal')
    expect(workspace).not.toHaveClass('is-expanded')
    fireEvent.click(screen.getByRole('button', { name: 'Show panel' }))
    expect(workspace).not.toHaveClass('is-minimal')
    fireEvent.click(screen.getByRole('button', { name: 'Full screen' }))
    expect(workspace).toHaveClass('is-expanded')
    fireEvent.click(screen.getByRole('button', { name: 'Exit full screen' }))
    expect(workspace).not.toHaveClass('is-expanded')
  })

  it('folds the study groups away behind an arrow and remembers the choice', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response([group]))
    const view = renderPage(<StudyRoomsPage />)
    const hide = await screen.findByRole('button', { name: 'Hide study groups' })
    expect(hide).toHaveAttribute('aria-expanded', 'true')
    hide.focus()
    fireEvent.click(hide)
    expect(screen.queryByRole('link', { name: 'Browse study groups' })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Show study groups' })).toHaveFocus()
    expect(localStorage.getItem('study-room-groups-collapsed')).toBe('true')
    view.unmount()
    renderPage(<StudyRoomsPage />)
    fireEvent.click(await screen.findByRole('button', { name: 'Show study groups' }))
    expect(await screen.findByRole('link', { name: 'Browse study groups' })).toBeInTheDocument()
    localStorage.clear()
  })

  it('lets the personal room choose its own weather and time of day for the background', async () => {
    vi.spyOn(api, 'get').mockImplementation(async path => response(path === '/weather' ? { available: false } : [group]))
    renderPage(<StudyRoomsPage />)
    fireEvent.click(await screen.findByRole('button', { name: 'Settings' }))
    fireEvent.click(screen.getByRole('tab', { name: 'Room atmosphere' }))
    fireEvent.change(screen.getByLabelText('Window view'), { target: { value: 'RAIN' } })
    fireEvent.change(screen.getByLabelText('Time of day'), { target: { value: 'NIGHT' } })
    const backdrop = document.querySelector('.atmosphere-backdrop')
    expect(backdrop).toHaveAttribute('data-weather', 'rain')
    expect(backdrop).toHaveAttribute('data-night', 'true')
    localStorage.clear()
  })

  it('keeps Study Room selected when opening an existing group room route', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response({ count: 0 }))
    openApplication('/groups/invalid/room')
    expect(await screen.findByText('This room link is invalid.')).toBeInTheDocument()
    const navigation = within(screen.getByRole('navigation', { name: 'Main navigation' }))
    expect(navigation.getByRole('link', { name: 'Study Room' })).toHaveAttribute('aria-current', 'page')
    expect(navigation.getByRole('link', { name: 'Study Room' })).toHaveClass('active')
    expect(navigation.getByRole('link', { name: 'Study groups' })).not.toHaveClass('active')
    expect(navigation.getByRole('link', { name: 'Study groups' })).not.toHaveAttribute('aria-current', 'page')
  })

  it('requires sign-in before requesting the private room list', async () => {
    const get = vi.spyOn(api, 'get')
    const router = openApplication('/study-room', null)
    expect(await screen.findByRole('heading', { level: 1, name: 'Log in' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/login')
    expect(get).not.toHaveBeenCalled()
  })
})
