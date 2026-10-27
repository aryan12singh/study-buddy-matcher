/** The parts of Spotify's Web Playback SDK this app uses. Spotify publishes no npm types for it. */
export type SpotifyPlayerState = {
  paused: boolean
  /** Both in milliseconds. */
  position: number
  duration: number
  track_window: {
    current_track: {
      name: string
      artists: { name: string }[]
      album: { images: { url: string }[] }
    }
  }
}

export interface SpotifyPlayer {
  connect(): Promise<boolean>
  disconnect(): void
  addListener(event: 'ready' | 'not_ready', callback: (device: { device_id: string }) => void): boolean
  addListener(event: 'player_state_changed', callback: (state: SpotifyPlayerState | null) => void): boolean
  addListener(event: 'initialization_error' | 'authentication_error' | 'account_error' | 'playback_error',
    callback: (error: { message: string }) => void): boolean
  togglePlay(): Promise<void>
  nextTrack(): Promise<void>
  previousTrack(): Promise<void>
  setVolume(volume: number): Promise<void>
  seek(positionMs: number): Promise<void>
  activateElement(): Promise<void>
}

type SpotifyPlayerOptions = {
  name: string
  getOAuthToken: (callback: (token: string) => void) => void
  volume?: number
}

type SpotifySdk = { Player: new (options: SpotifyPlayerOptions) => SpotifyPlayer }

declare global {
  interface Window {
    Spotify?: SpotifySdk
    onSpotifyWebPlaybackSDKReady?: () => void
  }
}

const SDK_URL = 'https://sdk.scdn.co/spotify-player.js'
let loading: Promise<SpotifySdk> | undefined

/** Loads Spotify's player script once per page load; it cannot be bundled. */
export function loadSpotifySdk() {
  loading ??= new Promise<SpotifySdk>((resolve, reject) => {
    if (window.Spotify) return resolve(window.Spotify)
    window.onSpotifyWebPlaybackSDKReady = () => window.Spotify ? resolve(window.Spotify) : reject(new Error('Spotify\'s player did not start.'))
    const script = document.createElement('script')
    script.src = SDK_URL
    script.async = true
    script.onerror = () => {
      loading = undefined
      script.remove()
      reject(new Error('Could not load Spotify\'s player. Check your connection and try again.'))
    }
    document.body.appendChild(script)
  })
  return loading
}
