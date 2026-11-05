import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import SpotifyPanel from './SpotifyPanel'
import { SpotifyContext, type SpotifyController } from './spotifyContext'

function controller(status: Partial<SpotifyController['status']>): SpotifyController {
  return {
    status: { data: undefined, loading: false, error: undefined, reload: vi.fn(), ...status } as SpotifyController['status'],
    mode: 'embed', chooseMode: vi.fn(), player: {} as SpotifyController['player'], volume: 50, setVolume: vi.fn(),
  }
}

function renderPanel(value: SpotifyController) {
  return render(<SpotifyContext.Provider value={value}><SpotifyPanel /></SpotifyContext.Provider>)
}

const READY = { configured: true, connected: false, embedPlaylistId: 'abc' }

describe('SpotifyPanel', () => {
  it('shows a loading state only before the first status arrives', () => {
    renderPanel(controller({ loading: true }))
    expect(screen.queryByTitle('Spotify study playlist')).toBeNull()
  })

  it('keeps the playlist playing while a refresh after a room action is in flight', () => {
    renderPanel(controller({ loading: true, data: READY }))
    expect(screen.getByTitle('Spotify study playlist')).toBeInTheDocument()
  })
})
