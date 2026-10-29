import { afterEach, describe, expect, it, vi } from 'vitest'
import { screen, within } from '@testing-library/react'
import { api } from '../../shared/api/client'
import { adminAccount, renderPage, response } from '../../test/renderApp'
import ForestPage from './ForestPage'
import type { Forest } from './api'

const stages: Forest['stages'] = [
  { species: 'SPROUT', minutes: 10 }, { species: 'SAPLING', minutes: 25 }, { species: 'OAK', minutes: 50 },
  { species: 'PINE', minutes: 90 }, { species: 'ANCIENT_BLOSSOM', minutes: 150 },
]
const forest: Forest = {
  treeCount: 2, focusedMinutes: 195, longestMinutes: 160, stages,
  trees: [
    { id: 2, species: 'ANCIENT_BLOSSOM', light: 'NIGHT', focusedMinutes: 160, startedAt: '2026-10-28T14:00:00Z', plantedAt: '2026-10-28T16:40:00Z', groupName: 'Revision crew' },
    { id: 1, species: 'SAPLING', light: 'DAY', focusedMinutes: 35, startedAt: '2026-10-27T04:00:00Z', plantedAt: '2026-10-27T04:40:00Z', groupName: 'IS442 grinders' },
  ],
}

afterEach(() => { vi.restoreAllMocks() })

describe('forest page', () => {
  it('shows one tree per visit with its species, focus time, group and totals', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response(forest))
    renderPage(<ForestPage />)
    const meadow = await screen.findByRole('region', { name: 'Meadow' })
    const trees = within(meadow).getAllByRole('listitem')
    expect(trees).toHaveLength(2)
    expect(trees[0]).toHaveTextContent('Ancient blossom')
    expect(trees[0]).toHaveTextContent('2 h 40 min')
    expect(trees[0]).toHaveTextContent('Revision crew, night')
    expect(trees[1]).toHaveTextContent('Sapling')
    expect(screen.getByText('Trees planted').nextSibling).toHaveTextContent('2')
    expect(screen.getByText('Focused time').nextSibling).toHaveTextContent('3 h 15 min')
    expect(within(screen.getByRole('region', { name: 'How trees grow' })).getAllByRole('listitem')).toHaveLength(5)
  })

  it('explains how to plant a first tree when the forest is empty', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response({ ...forest, trees: [], treeCount: 0, focusedMinutes: 0, longestMinutes: 0 }))
    renderPage(<ForestPage />)
    expect(await screen.findByText('Your forest is a seed')).toBeInTheDocument()
    expect(screen.getByText(/10 focused minutes plants your first tree/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Open study rooms' })).toHaveAttribute('href', '/study-room')
  })

  it('shows an error with retry, and never loads a forest for an administrator', async () => {
    const get = vi.spyOn(api, 'get').mockRejectedValue(new Error('Network down'))
    const { unmount } = renderPage(<ForestPage />)
    expect(await screen.findByRole('alert')).toHaveTextContent('Network down')
    expect(screen.getByRole('button', { name: 'Try again' })).toBeInTheDocument()
    unmount()
    get.mockClear()
    renderPage(<ForestPage />, '/', '/', adminAccount)
    expect(get).not.toHaveBeenCalled()
  })
})
