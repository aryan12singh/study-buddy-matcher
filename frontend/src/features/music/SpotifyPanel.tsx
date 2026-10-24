import { useSpotify } from './spotifyContext'
import SpotifyEmbed from './SpotifyEmbed'
import SpotifyPremiumPlayer from './SpotifyPremiumPlayer'
import StatePanel from '../../shared/components/StatePanel'

/** Lets the student choose between their Premium account and the free embedded playlist. */
export default function SpotifyPanel() {
  const { status, mode, chooseMode: choose } = useSpotify()

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
