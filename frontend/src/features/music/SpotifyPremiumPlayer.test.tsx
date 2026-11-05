import { render, screen, waitFor } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { api } from '../../shared/api/client'
import SpotifyPremiumPlayer from './SpotifyPremiumPlayer'
import { SpotifyContext, type SpotifyController } from './spotifyContext'

const STATUS = { configured: true, connected: false, embedPlaylistId: 'abc' }

function openReturnedFromSpotify(search: string) {
  const reload = vi.fn()
  const controller = {
    status: { data: STATUS, loading: false, error: undefined, reload }, mode: 'premium', chooseMode: vi.fn(),
    player: {}, volume: 50, setVolume: vi.fn(),
  } as unknown as SpotifyController
  const router = createMemoryRouter([{ path: '/study-room', element:
    <SpotifyContext.Provider value={controller}><SpotifyPremiumPlayer status={STATUS} /></SpotifyContext.Provider> }],
  { initialEntries: [`/study-room${search}`] })
  render(<RouterProvider router={router} />)
  return { router, reload }
}

describe('SpotifyPremiumPlayer', () => {
  afterEach(() => vi.restoreAllMocks())

  it('finishes a returned sign-in as the signed-in student and drops the code from the address', async () => {
    const post = vi.spyOn(api, 'post').mockResolvedValue({ data: { outcome: 'connected' } })
    const { router, reload } = openReturnedFromSpotify('?spotify=confirm&state=s1&code=c1')

    expect(await screen.findByText('Spotify connected.')).toBeInTheDocument()
    expect(post).toHaveBeenCalledOnce()
    expect(post).toHaveBeenCalledWith('/spotify/authorization/complete', { state: 's1', code: 'c1' })
    expect(router.state.location.search).toBe('')
    expect(reload).toHaveBeenCalled()
  })

  it('says the sign-in expired when it was not this student who started it', async () => {
    vi.spyOn(api, 'post').mockResolvedValue({ data: { outcome: 'expired' } })
    openReturnedFromSpotify('?spotify=confirm&state=someone-elses&code=c1')

    await waitFor(() => expect(screen.getByText('That Spotify sign-in expired. Try connecting again.')).toBeInTheDocument())
  })
})
