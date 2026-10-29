import { afterEach, describe, expect, it, vi } from 'vitest'
import { act, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { RouterProvider } from 'react-router-dom'
import { api, refreshResources } from '../../shared/api/client'
import { AuthContext } from '../../shared/auth/context'
import { adminAccount, deferred, renderPage, response, studentAccount } from '../../test/renderApp'
import { achievementStanding } from '../../test/achievementStanding'
import AchievementsPage from './AchievementsPage'

afterEach(() => { vi.restoreAllMocks() })

describe('achievements page', () => {
  it('shows current tier, next tier and accessible progress with actionable existing routes', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response(achievementStanding))
    renderPage(<AchievementsPage />)
    const earned = await screen.findByRole('listitem', { name: 'Connector, Silver, 3 of 5 toward Gold' })
    expect(within(earned).getByText('2 more active connections to reach Gold.')).toBeInTheDocument()
    expect(within(earned).getByRole('progressbar')).toHaveAttribute('aria-valuenow', '3')
    expect(screen.getByRole('listitem', { name: 'Founder, Locked, 0 of 1 toward Bronze' })).toHaveClass('badge-locked')
    expect(screen.getByRole('list', { name: '3 week planned-session streak' })).toBeInTheDocument()
    expect(screen.getByRole('progressbar', { name: 'Current badge points' })).toHaveAttribute('aria-valuenow', '5')
    expect(screen.getByRole('progressbar', { name: 'Current badge points' })).toHaveAttribute('aria-valuemax', '18')
    expect(screen.getByText('Week of 19 Oct: sapling')).toBeInTheDocument()
    for (const [name, href] of [
      ['View connections', '/connections?view=connected'], ['Find buddies to request', '/find-buddies'],
      ['Browse study groups', '/groups'], ['Create a study group', '/groups/new'],
      ['Open study sessions', '/sessions'], ['Plan your next week', '/sessions']
    ]) expect(screen.getByRole('link', { name })).toHaveAttribute('href', href)
    expect(api.get).toHaveBeenCalledWith('/achievements/me', expect.objectContaining({ signal: expect.any(AbortSignal) }))
  })

  it('explains current records, planning and a session later this week honestly', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response(achievementStanding))
    renderPage(<AchievementsPage />)
    await screen.findByRole('heading', { name: 'Current standing' })
    expect(screen.getByText(/Badge tiers you reach are kept, so your level never goes down/)).toBeInTheDocument()
    expect(screen.getByText(/weeks starting on Monday/)).toHaveTextContent('A session later this week counts now; future weeks wait until they begin.')
    fireEvent.click(screen.getByText('How these numbers work'))
    expect(screen.getByText(/from your current connection or membership start date onward/)).toBeVisible()
    expect(screen.getByText(/does not record attendance, completed focus time or study quality/)).toBeVisible()
  })

  it('keeps an earned tier and says what the current count reaches when it has fallen', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response({ ...achievementStanding,
      badges: achievementStanding.badges.map(badge => badge.key === 'CONNECTOR'
        ? { ...badge, tier: 'SILVER', currentTier: null, current: 0, target: 5, nextTier: 'GOLD', earnedHint: '5 more active connections to reach Gold.' }
        : badge) }))
    renderPage(<AchievementsPage />)
    const connector = await screen.findByRole('listitem', { name: 'Connector, Silver, 0 of 5 toward Gold' })
    expect(connector).toHaveClass('badge-silver')
    expect(within(connector).getByText('Silver stays earned. Your current count alone reaches no tier.')).toBeInTheDocument()
    const firstMove = screen.getByRole('listitem', { name: 'First Move, Bronze, 1 of 5 toward Silver' })
    expect(within(firstMove).queryByText(/stays earned/)).not.toBeInTheDocument()
  })

  it('shows a seed and working actions for a new student with all badges locked', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response({ ...achievementStanding, earnedCount: 0,
      streak: { ...achievementStanding.streak, currentWeeks: 0, bestWeeks: 0, currentRunWeeks: [] },
      badges: achievementStanding.badges.map(badge => ({ ...badge, current: 0, target: 1, tier: null, earned: false, nextTier: 'BRONZE' })) }))
    const { router } = renderPage(<AchievementsPage />)
    expect(await screen.findByRole('heading', { name: 'Your grove is a seed' })).toBeInTheDocument()
    const grove = screen.getByRole('region', { name: 'Planned-session grove' })
    fireEvent.click(within(grove).getByRole('link', { name: 'Open study sessions' }))
    await waitFor(() => expect(router.state.location.pathname).toBe('/sessions'))
  })

  it('caps gold progress and stops promising an unreachable level', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response({ ...achievementStanding,
      level: 7, points: 18, pointsToNextLevel: 0, earnedCount: 6,
      badges: achievementStanding.badges.map(badge => ({ ...badge, tier: 'GOLD', nextTier: null, current: 99, target: 5,
        earned: true, earnedHint: 'Gold earned. No higher tier.' })) }))
    renderPage(<AchievementsPage />)
    expect(await screen.findByText('Maximum level · all badges Gold')).toBeInTheDocument()
    expect(screen.queryByText(/to level 8/)).not.toBeInTheDocument()
    const connector = screen.getByRole('progressbar', { name: 'Connector Gold progress' })
    expect(connector).toHaveAttribute('aria-valuenow', '5')
    expect(connector).toHaveAttribute('aria-valuetext', 'Gold earned; current count 99')
    expect(screen.getAllByText('· Gold at 5')).toHaveLength(6)
  })

  it('renders at most twelve dated trees and states the full streak length', async () => {
    const weeks = Array.from({ length: 14 }, (_, index) => new Date(Date.UTC(2026, 6, 20 + index * 7)).toISOString().slice(0, 10))
    vi.spyOn(api, 'get').mockResolvedValue(response({ ...achievementStanding,
      streak: { ...achievementStanding.streak, currentWeeks: 14, bestWeeks: 14, currentRunWeeks: weeks } }))
    renderPage(<AchievementsPage />)
    const grove = await screen.findByRole('list', { name: '14 week planned-session streak' })
    expect(within(grove).getAllByRole('listitem')).toHaveLength(12)
    expect(screen.getByText('Showing the latest 12 of 14 weeks.')).toBeInTheDocument()
  })

  it('offers an action even if the badge catalogue is empty', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response({ ...achievementStanding, badges: [] }))
    renderPage(<AchievementsPage />)
    expect(await screen.findByRole('heading', { name: 'No badges available' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Find a study buddy' })).toHaveAttribute('href', '/find-buddies')
  })

  it('shows loading, supports retry after failure, and purges old data on a failed refresh', async () => {
    const pending = deferred<never>()
    vi.spyOn(api, 'get').mockImplementationOnce(() => pending.promise)
      .mockRejectedValueOnce(new Error('Network down')).mockResolvedValue(response(achievementStanding))
    renderPage(<AchievementsPage />)
    await screen.findByRole('status')
    await act(async () => { pending.resolve(response(achievementStanding)) })
    await screen.findByRole('heading', { name: 'Current standing' })
    act(() => refreshResources(true))
    expect(await screen.findByRole('alert')).toHaveTextContent('Network down')
    expect(screen.queryByRole('heading', { name: 'Current standing' })).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Try again' }))
    await screen.findByRole('heading', { name: 'Current standing' })
  })

  it('aborts a previous owner request and hides old standing on account change and logout', async () => {
    const old = deferred<never>(), next = deferred<never>()
    const get = vi.spyOn(api, 'get').mockImplementationOnce(() => old.promise).mockImplementationOnce(() => next.promise)
    const { auth, router, rerender } = renderPage(<AchievementsPage />)
    await waitFor(() => expect(get).toHaveBeenCalledTimes(1))
    const firstSignal = get.mock.calls[0][1]?.signal as AbortSignal
    rerender(<AuthContext.Provider value={{ ...auth, account: { ...studentAccount, id: 2 } }}><RouterProvider router={router} /></AuthContext.Provider>)
    await waitFor(() => expect(get).toHaveBeenCalledTimes(2))
    expect(firstSignal.aborted).toBe(true)
    await act(async () => { old.resolve(response(achievementStanding)) })
    expect(screen.queryByRole('heading', { name: 'Current standing' })).not.toBeInTheDocument()
    await act(async () => { next.resolve(response(achievementStanding)) })
    await screen.findByRole('heading', { name: 'Current standing' })
    rerender(<AuthContext.Provider value={{ ...auth, account: null }}><RouterProvider router={router} /></AuthContext.Provider>)
    expect(screen.queryByRole('heading', { name: 'Current standing' })).not.toBeInTheDocument()
    expect(get).toHaveBeenCalledTimes(2)
  })

  it('does not fetch achievements for an administrator', () => {
    const get = vi.spyOn(api, 'get')
    renderPage(<AchievementsPage />, '/', '/', adminAccount)
    expect(get).not.toHaveBeenCalled()
  })

  it('shows the Forester badge with a way to grow a tree, and a link to the forest once earned', async () => {
    const forester = { key: 'FORESTER' as const, title: 'Forester', description: 'Trees in your focus forest.', tier: null,
      currentTier: null, nextTier: 'BRONZE' as const, current: 0, target: 1, earned: false, earnedHint: '1 more tree to reach Bronze.' }
    vi.spyOn(api, 'get').mockResolvedValue(response({ ...achievementStanding, badges: [...achievementStanding.badges, forester] }))
    renderPage(<AchievementsPage />)
    expect(await screen.findByRole('link', { name: 'Grow a tree in a study room' })).toHaveAttribute('href', '/study-room')
    expect(within(screen.getByRole('region', { name: 'Planned-session grove' })).getByRole('link', { name: 'View my forest' }))
      .toHaveAttribute('href', '/forest')
  })
})
