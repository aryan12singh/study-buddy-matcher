import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { getSpotifyStatus } from './spotifyApi'
import SpotifyEmbed from './SpotifyEmbed'
import SpotifyPremiumPlayer from './SpotifyPremiumPlayer'
import { useResource } from '../../shared/api/useResource'
import StatePanel from '../../shared/components/StatePanel'

type Mode = 'premium' | 'embed'
const MODE_KEY = 'spotify-player-mode'

function readMode(): Mode {
  try { return localStorage.getItem(MODE_KEY) === 'premium' ? 'premium' : 'embed' } catch { return 'embed' }
}

/** Lets the student choose between their Premium account and the free embedded playlist. */
export default function SpotifyPanel() {
  const [search] = useSearchParams()
  // Coming back from Spotify sign-in means the student wants the Premium player.
  const [mode, setMode] = useState<Mode>(() => search.has('spotify') ? 'premium' : readMode())
  const status = useResource('spotify-status', getSpotifyStatus, true, false)

  function choose(next: Mode) {
    setMode(next)
    try { localStorage.setItem(MODE_KEY, next) } catch { /* Storage is optional. */ }
  }

  if (status.loading) return <StatePanel loading />
  if (status.error || !status.data) return <StatePanel error={status.error} onRetry={() => status.reload(true)} />

  return (
    <div className="spotify-panel">
      <div className="music-mode" role="group" aria-label="Spotify player">
        <button type="button" aria-pressed={mode === 'embed'} onClick={() => choose('embed')}>Free playlist</button>
        <button type="button" aria-pressed={mode === 'premium'} onClick={() => choose('premium')}>My Spotify (Premium)</button>
      </div>
      {mode === 'embed'
        ? status.data.embedPlaylistId
          ? <SpotifyEmbed playlistId={status.data.embedPlaylistId} />
          : <p className="muted">No free playlist is set up on this server.</p>
        : <SpotifyPremiumPlayer status={status.data} />}
    </div>
  )
}
