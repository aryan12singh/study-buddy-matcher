import { afterEach, describe, expect, it, vi } from 'vitest'
import { screen, within } from '@testing-library/react'
import { api } from '../../shared/api/client'
import { renderPage, response } from '../../test/renderApp'
import AchievementsPage from './AchievementsPage'
import type { Achievements, Badge } from './api'

afterEach(() => { vi.restoreAllMocks() })

const badge = (overrides: Partial<Badge>): Badge => ({ key: 'CONNECTOR', title: 'Connector', description: 'Active study connections',
  tier: null, nextTier: 'BRONZE', current: 0, target: 1, earned: false, earnedHint: '1 more connection to reach Bronze.', ...overrides })

const withGrove: Achievements = {
  level: 2, points: 3, pointsToNextLevel: 3, pointsPerLevel: 3, earnedCount: 2, totalCount: 2,
  streak: { currentWeeks: 3, bestWeeks: 4, currentRunWeeks: ['2026-10-05', '2026-10-12', '2026-10-19'] },
  badges: [
    badge({ tier: 'SILVER', nextTier: 'GOLD', current: 3, target: 5, earned: true, earnedHint: '2 more connections to reach Gold.' }),
    badge({ key: 'FOUNDER', title: 'Founder', description: 'Study groups you created', current: 0, target: 1, earnedHint: '1 more group to create to reach Bronze.' })
  ]
}

describe('achievements page', () => {
  it('shows earned and locked badges with their progress', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response(withGrove))
    renderPage(<AchievementsPage />)
    const earned = await screen.findByRole('listitem', { name: 'Connector, Silver, 3 of 5' })
    expect(within(earned).getByText('2 more connections to reach Gold.')).toBeInTheDocument()
    const locked = screen.getByRole('listitem', { name: 'Founder, Locked, 0 of 1' })
    expect(locked).toHaveClass('badge-locked')
    expect(screen.getByRole('list', { name: '3 week study streak' })).toBeInTheDocument()
    expect(screen.getByText('Week of 19 Oct')).toBeInTheDocument()
    expect(api.get).toHaveBeenCalledWith('/achievements/me', expect.anything())
  })

  it('shows a seed and how to grow the grove when there is no streak', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response({ ...withGrove, streak: { currentWeeks: 0, bestWeeks: 0, currentRunWeeks: [] } }))
    renderPage(<AchievementsPage />)
    expect(await screen.findByRole('heading', { name: 'Your grove is a seed' })).toBeInTheDocument()
    expect(screen.getByText(/Schedule a study session/)).toBeInTheDocument()
  })

  it('shows the error state with a retry', async () => {
    vi.spyOn(api, 'get').mockRejectedValue(new Error('Network down'))
    renderPage(<AchievementsPage />)
    expect(await screen.findByRole('alert')).toHaveTextContent('Unable to load this view')
    expect(screen.getByRole('button', { name: 'Try again' })).toBeInTheDocument()
  })
})
