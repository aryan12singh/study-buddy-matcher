import { afterEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, screen, within } from '@testing-library/react'
import { api } from '../../shared/api/client'
import { adminAccount, renderPage, response } from '../../test/renderApp'
import ForestPage from './ForestPage'
import type { Forest } from './api'

const stages: Forest['stages'] = [
  { species: 'SPROUT', minutes: 10 }, { species: 'SAPLING', minutes: 25 }, { species: 'OAK', minutes: 50 },
  { species: 'PINE', minutes: 90 }, { species: 'ANCIENT_BLOSSOM', minutes: 150 },
]
const forest: Forest = {
  treeCount: 2, focusedMinutes: 195, longestMinutes: 160, stages, variantsPerSpecies: 3,
  trees: [
    { id: 2, species: 'ANCIENT_BLOSSOM', light: 'NIGHT', focusedMinutes: 160, startedAt: '2026-10-28T14:00:00Z', plantedAt: '2026-10-28T16:40:00Z', groupName: 'Revision crew', variant: 0, plot: 40 },
    { id: 1, species: 'SAPLING', light: 'DAY', focusedMinutes: 35, startedAt: '2026-10-27T04:00:00Z', plantedAt: '2026-10-27T04:40:00Z', groupName: 'IS442 grinders', variant: 1, plot: 40 },
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
    expect(trees[1]).toHaveTextContent('Birch sapling')
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

  it('stands every tree in the 3D meadow as a labelled button and shows its details when chosen', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response(forest))
    renderPage(<ForestPage />)
    const stage = await screen.findByRole('group', { name: /3D forest/ })
    const birch = within(stage).getByRole('button', { name: /^Birch sapling, 35 min focused in IS442 grinders/ })
    expect(within(stage).getAllByRole('button')).toHaveLength(2)
    expect(screen.getByText('Select a tree to see when and where it grew.')).toBeInTheDocument()
    fireEvent.click(birch)
    expect(screen.getByText('Birch sapling', { selector: '.meadow-detail strong' })).toBeInTheDocument()
    // The near edge follows the key, pointer or swipe: right lowers the clockwise spin angle.
    fireEvent.keyDown(stage, { key: 'ArrowRight' })
    expect(stage.style.getPropertyValue('--spin')).toBe('-50deg')
    fireEvent.click(screen.getByRole('button', { name: '◀ Turn left' }))
    expect(stage.style.getPropertyValue('--spin')).toBe('-20deg')
    fireEvent.pointerDown(stage, { clientX: 100, clientY: 100, pointerId: 1 })
    fireEvent.pointerMove(stage, { clientX: 200, clientY: 100, pointerId: 1 })
    fireEvent.pointerUp(stage, { pointerId: 1 })
    expect(stage.style.getPropertyValue('--spin')).toBe('-65deg')
    fireEvent.wheel(stage, { deltaX: -100, deltaY: 0 })
    expect(stage.style.getPropertyValue('--spin')).toBe('-95deg')
    fireEvent.click(screen.getByRole('button', { name: 'Reset view' }))
    expect(stage.style.getPropertyValue('--spin')).toBe('-35deg')
  })
})
