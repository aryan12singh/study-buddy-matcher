import { useEffect, useRef, useState } from 'react'
import { getSpotifyAccessToken } from './spotifyApi'
import { loadSpotifySdk } from './spotifySdk'
import type { SpotifyPlayer, SpotifyPlayerState } from './spotifySdk'
import { errorMessage } from '../../shared/api/client'

export type NowPlaying = {
  name: string
  artists: string
  imageUrl?: string
  paused: boolean
}

type PlayerState = {
  status: 'off' | 'starting' | 'ready' | 'error'
  deviceId?: string
  nowPlaying?: NowPlaying
  error?: string
}

const PLAYER_NAME = 'Study Buddy Matcher'
const PREMIUM_REQUIRED = 'Spotify only plays music here for Premium accounts.'

function nowPlayingFrom(state: SpotifyPlayerState | null): NowPlaying | undefined {
  const track = state?.track_window.current_track
  return state && track ? {
    name: track.name,
    artists: track.artists.map(artist => artist.name).join(', '),
    imageUrl: track.album.images[0]?.url,
    paused: state.paused,
  } : undefined
}

/**
 * A Spotify player that lives in this browser tab while the student is connected. Spotify sees it
 * as a device called "Study Buddy Matcher". It disconnects when the page is left.
 */
export function useSpotifyPlayer(connected: boolean) {
  const player = useRef<SpotifyPlayer | null>(null)
  const [state, setState] = useState<PlayerState>({ status: 'off' })

  useEffect(() => {
    if (!connected) return
    let disposed = false
    const update = (change: Partial<PlayerState>) => { if (!disposed) setState(previous => ({ ...previous, ...change })) }
    const fail = (message: string) => update({ status: 'error', error: message })
    queueMicrotask(() => update({ status: 'starting', error: undefined }))
    loadSpotifySdk().then(sdk => {
      if (disposed) return
      const created = new sdk.Player({
        name: PLAYER_NAME,
        // Spotify asks again whenever its token is about to expire; the backend renews it.
        getOAuthToken: callback => { getSpotifyAccessToken().then(token => callback(token.accessToken), error => fail(errorMessage(error))) },
      })
      created.addListener('ready', ({ device_id }) => update({ status: 'ready', deviceId: device_id }))
      created.addListener('not_ready', () => update({ status: 'starting', deviceId: undefined }))
      created.addListener('player_state_changed', playerState => update({ nowPlaying: nowPlayingFrom(playerState) }))
      created.addListener('account_error', () => fail(PREMIUM_REQUIRED))
      created.addListener('authentication_error', () => fail('Spotify did not accept the connection. Disconnect Spotify and connect it again.'))
      created.addListener('initialization_error', () => fail('This browser cannot run Spotify\'s player.'))
      created.addListener('playback_error', () => update({ error: 'Spotify could not play that. Try another playlist.' }))
      player.current = created
      void created.connect()
    }, error => fail(errorMessage(error)))
    return () => {
      disposed = true
      player.current?.disconnect()
      player.current = null
      setState({ status: 'off' })
    }
  }, [connected])

  return {
    ...state,
    /** Must run inside a click, or some browsers block the sound that follows. */
    activate: () => player.current?.activateElement() ?? Promise.resolve(),
    togglePlay: () => void player.current?.togglePlay(),
    nextTrack: () => void player.current?.nextTrack(),
    previousTrack: () => void player.current?.previousTrack(),
    setVolume: (volume: number) => void player.current?.setVolume(volume),
  }
}
