import { useCallback, useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { useSearchParams } from 'react-router-dom'
import { getSpotifyStatus } from './spotifyApi'
import { SpotifyContext } from './spotifyContext'
import { AmbientContext } from './ambientContext'
import { MusicSettingsContext } from './musicSettingsContext'
import { useAmbientMixer } from './useAmbientMixer'
import type { SpotifyMode } from './spotifyContext'
import { DEFAULT_VOLUME, useSpotifyPlayer } from './useSpotifyPlayer'
import { useResource } from '../../shared/api/useResource'
import { openRoomSettings } from '../studyroom/roomSettingsEvents'

const MODE_KEY = 'spotify-player-mode'

function readMode(): SpotifyMode {
  try { return localStorage.getItem(MODE_KEY) === 'premium' ? 'premium' : 'embed' } catch { return 'embed' }
}

/**
 * Holds the page's Spotify player and ambient mixer above the Music settings tab, so the timer's mini
 * player controls the same playback. The Premium player only starts in Premium mode for a
 * connected student; every sound stops when the page is left.
 */
export default function MusicProvider({ children }: { children: ReactNode }) {
  const [search] = useSearchParams()
  // Coming back from Spotify sign-in means the student wants the Premium player.
  const [mode, setMode] = useState<SpotifyMode>(() => search.has('spotify') ? 'premium' : readMode())
  const status = useResource('spotify-status', getSpotifyStatus, true, false)
  const player = useSpotifyPlayer(Boolean(status.data?.connected) && mode === 'premium')
  const [volume, setVolumeState] = useState(DEFAULT_VOLUME)
  const mixer = useAmbientMixer()
  const [settingsOpened, setSettingsOpened] = useState(false)
  const [returnedFromSpotify] = useState(() => search.has('spotify'))
  const markOpened = useCallback(() => setSettingsOpened(true), [])

  // Spotify sign-in returns with ?spotify=, so open the music tab to show the outcome. The menu
  // is a child, so it is already listening when this runs.
  useEffect(() => { if (returnedFromSpotify) openRoomSettings('music') }, [returnedFromSpotify])

  function chooseMode(next: SpotifyMode) {
    setMode(next)
    try { localStorage.setItem(MODE_KEY, next) } catch { /* Storage is optional. */ }
  }

  function setVolume(next: number) {
    setVolumeState(next)
    player.setVolume(next)
  }

  return (
    <SpotifyContext.Provider value={{ status, mode, chooseMode, player, volume, setVolume }}>
      <AmbientContext.Provider value={mixer}>
        <MusicSettingsContext.Provider value={{ opened: settingsOpened, markOpened }}>
          {children}
        </MusicSettingsContext.Provider>
      </AmbientContext.Provider>
    </SpotifyContext.Provider>
  )
}
