import { afterEach, describe, expect, it, vi } from 'vitest'
import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router-dom'
import { appRoutes } from '../../routes'
import { api } from '../../shared/api/client'
import { AuthContext } from '../../shared/auth/context'
import type { Account } from '../../shared/auth/context'
import { deferred, renderPage, response, studentAccount } from '../../test/renderApp'
import type { GroupSummary } from '../groups/api'
import StudyRoomsPage from './StudyRoomsPage'
import MusicProvider from '../music/MusicProvider'

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
    renderPage(<MusicProvider><StudyRoomsPage /></MusicProvider>)
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
    renderPage(<MusicProvider><StudyRoomsPage /></MusicProvider>)
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
    const view = renderPage(<MusicProvider><StudyRoomsPage /></MusicProvider>)
    const hide = await screen.findByRole('button', { name: 'Hide study groups' })
    expect(hide).toHaveAttribute('aria-expanded', 'true')
    hide.focus()
    fireEvent.click(hide)
    expect(screen.queryByRole('link', { name: 'Browse study groups' })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Show study groups' })).toHaveFocus()
    expect(localStorage.getItem('study-room-groups-collapsed')).toBe('true')
    view.unmount()
    renderPage(<MusicProvider><StudyRoomsPage /></MusicProvider>)
    fireEvent.click(await screen.findByRole('button', { name: 'Show study groups' }))
    expect(await screen.findByRole('link', { name: 'Browse study groups' })).toBeInTheDocument()
    localStorage.clear()
  })

  it('lets the personal room choose its own weather and time of day for the background', async () => {
    vi.spyOn(api, 'get').mockImplementation(async path => response(path === '/weather' ? { available: false } : [group]))
    renderPage(<MusicProvider><StudyRoomsPage /></MusicProvider>)
    fireEvent.click(await screen.findByRole('button', { name: 'Settings' }))
    fireEvent.click(screen.getByRole('tab', { name: 'Room atmosphere' }))
    fireEvent.change(screen.getByLabelText('Window view'), { target: { value: 'RAIN' } })
    fireEvent.change(screen.getByLabelText('Time of day'), { target: { value: 'NIGHT' } })
    const backdrop = document.querySelector('.atmosphere-backdrop')
    expect(backdrop).toHaveAttribute('data-weather', 'rain')
    expect(backdrop).toHaveAttribute('data-night', 'true')
    localStorage.clear()
  })

  it('keeps the Spotify player connected while the student visits another page and comes back', async () => {
    const created = vi.fn(), disconnect = vi.fn()
    class FakePlayer {
      constructor() { created() }
      addListener(event: string, callback: (device: { device_id: string }) => void) {
        if (event === 'ready') queueMicrotask(() => callback({ device_id: 'device-1' }))
        return true
      }
      connect = vi.fn(async () => true)
      disconnect = disconnect
    }
    window.Spotify = { Player: FakePlayer } as never
    localStorage.setItem('spotify-player-mode', 'premium')
    vi.spyOn(api, 'get').mockImplementation(async path => response(
      path === '/spotify/status' ? { configured: true, connected: true, embedPlaylistId: 'abc' }
        : path === '/notifications/unread-count' ? { count: 0 } : path === '/weather' ? { available: false }
          : path.startsWith('/groups') ? [group] : []))
    try {
      const router = openApplication('/study-room')
      expect(await screen.findByRole('heading', { level: 1, name: 'Study Room' })).toBeInTheDocument()
      await waitFor(() => expect(created).toHaveBeenCalledTimes(1))

      await act(async () => { await router.navigate('/notifications') })
      expect(await screen.findByRole('heading', { level: 1, name: 'Notifications' })).toBeInTheDocument()
      await act(async () => { await router.navigate('/study-room') })
      expect(await screen.findByRole('heading', { level: 1, name: 'Study Room' })).toBeInTheDocument()
      expect(created).toHaveBeenCalledTimes(1)
      expect(disconnect).not.toHaveBeenCalled()
    } finally {
      delete window.Spotify
      localStorage.clear()
    }
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

  it('invites the student to pick music from the timer corner and opens the Music settings tab', async () => {
    vi.spyOn(api, 'get').mockImplementation(async path => response(path === '/spotify/status'
      ? { configured: false, connected: false, embedPlaylistId: 'abc' } : path === '/ambient/categories' ? [] : [group]))
    renderPage(<MusicProvider><StudyRoomsPage /></MusicProvider>)
    const settings = await screen.findByRole('button', { name: 'Settings' })
    expect(settings).toHaveAttribute('aria-expanded', 'false')
    fireEvent.click(await screen.findByRole('button', { name: 'Open music settings' }))
    expect(settings).toHaveAttribute('aria-expanded', 'true')
    expect(screen.getByRole('tab', { name: 'Music' })).toHaveAttribute('aria-selected', 'true')
    expect(await screen.findByTitle('Spotify study playlist')).toBeInTheDocument()
    // Closing the menu hides the tab but keeps the player loaded, so music keeps playing.
    fireEvent.keyDown(screen.getByRole('tab', { name: 'Music' }), { key: 'Escape' })
    expect(settings).toHaveAttribute('aria-expanded', 'false')
    expect(screen.getByTitle('Spotify study playlist')).toBeInTheDocument()
    // The free playlist cannot report playback, so the prompt steps aside once it is loaded.
    await waitFor(() => expect(screen.queryByRole('button', { name: 'Open music settings' })).not.toBeInTheDocument())
  })

  it('requires sign-in before requesting the private room list', async () => {
    const get = vi.spyOn(api, 'get')
    const router = openApplication('/study-room', null)
    expect(await screen.findByRole('heading', { level: 1, name: 'Log in' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/login')
    expect(get).not.toHaveBeenCalled()
  })
})
