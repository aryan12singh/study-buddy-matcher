import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import NowPlayingMini from './NowPlayingMini'
import { AmbientContext } from './ambientContext'
import { MusicSettingsContext } from './musicSettingsContext'
import { SpotifyContext, type SpotifyController } from './spotifyContext'
import type { NowPlaying } from './useSpotifyPlayer'

const TRACK: NowPlaying = { name: 'Lo-fi beats', artists: 'Someone', paused: false, positionMs: 0, durationMs: 180000, reportedAt: 0 }

function openMiniPlayer(nowPlaying: NowPlaying) {
  const togglePlay = vi.fn()
  const spotify = {
    mode: 'premium', volume: 0.5, setVolume: vi.fn(),
    player: { status: 'ready', nowPlaying, togglePlay, nextTrack: vi.fn() },
  } as unknown as SpotifyController
  const ambient = { anyPlaying: false, master: 1, setMaster: vi.fn() } as never
  render(
    <SpotifyContext.Provider value={spotify}>
      <AmbientContext.Provider value={ambient}>
        <MusicSettingsContext.Provider value={{ opened: true, markOpened: vi.fn() }}>
          <NowPlayingMini />
        </MusicSettingsContext.Provider>
      </AmbientContext.Provider>
    </SpotifyContext.Provider>,
  )
  return togglePlay
}

describe('NowPlayingMini', () => {
  it('pauses a playing track from the corner player', () => {
    const togglePlay = openMiniPlayer(TRACK)
    expect(screen.getByText('Now playing')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Pause music' }))
    expect(togglePlay).toHaveBeenCalledOnce()
  })

  it('resumes a paused track with the same button', () => {
    const togglePlay = openMiniPlayer({ ...TRACK, paused: true })
    expect(screen.getByText('Paused')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Pause music' })).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Resume music' }))
    expect(togglePlay).toHaveBeenCalledOnce()
  })
})
