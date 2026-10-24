import { createContext, useContext } from 'react'
import type { useResource } from '../../shared/api/useResource'
import type { SpotifyStatus } from './spotifyApi'
import type { useSpotifyPlayer } from './useSpotifyPlayer'

export type SpotifyMode = 'premium' | 'embed'

/** One Spotify player per study room page, shared by the Music settings tab and the timer's mini player. */
export type SpotifyController = {
  status: ReturnType<typeof useResource<SpotifyStatus>>
  mode: SpotifyMode
  chooseMode: (mode: SpotifyMode) => void
  player: ReturnType<typeof useSpotifyPlayer>
  volume: number
  setVolume: (volume: number) => void
}

export const SpotifyContext = createContext<SpotifyController | null>(null)

export function useSpotify() {
  const controller = useContext(SpotifyContext)
  if (!controller) throw new Error('useSpotify must be used inside MusicProvider')
  return controller
}
