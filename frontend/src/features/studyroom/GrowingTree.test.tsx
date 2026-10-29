import { afterEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, screen } from '@testing-library/react'
import { api } from '../../shared/api/client'
import { renderPage, response } from '../../test/renderApp'
import GrowingTree from './GrowingTree'

const growing = { visiting: true, focusedMinutes: 30, species: 'SAPLING', variant: 1, next: 'OAK', minutesToNext: 20, light: 'DAY' }

afterEach(() => { vi.restoreAllMocks() })

describe('growing tree in a room', () => {
  it('names the drawn kind of tree and opens the forest in a new tab so the visit is not ended', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response(growing))
    renderPage(<GrowingTree groupId={5} accountId={1} />)
    expect(await screen.findByText('Birch sapling')).toBeInTheDocument()
    expect(screen.getByText('20 min more to grow into an apple tree.')).toBeInTheDocument()
    const forest = screen.getByRole('link', { name: 'View my forest (opens in a new tab)' })
    expect(forest).toHaveAttribute('target', '_blank')
    expect(forest).toHaveAttribute('href', '/forest')
  })

  it('shows loading, then an error with a retry instead of claiming no tree is growing', async () => {
    const get = vi.spyOn(api, 'get').mockReturnValueOnce(new Promise(() => {}))
    const { unmount } = renderPage(<GrowingTree groupId={5} accountId={1} />)
    expect(await screen.findByText('Checking on your tree…')).toBeInTheDocument()
    unmount()
    get.mockRejectedValueOnce(new Error('Network down')).mockResolvedValue(response(growing))
    renderPage(<GrowingTree groupId={5} accountId={1} />)
    expect(await screen.findByRole('alert')).toHaveTextContent('Network down')
    fireEvent.click(screen.getByRole('button', { name: 'Try again' }))
    expect(await screen.findByText('Birch sapling')).toBeInTheDocument()
  })
})
