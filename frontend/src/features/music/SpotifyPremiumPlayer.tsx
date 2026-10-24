import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { startSpotifyAuthorization } from './spotifyApi'
import type { SpotifyStatus } from './spotifyApi'
import { DEFAULT_VOLUME, useSpotifyPlayer } from './useSpotifyPlayer'
import { errorMessage } from '../../shared/api/client'
import ActionNotice from '../../shared/components/ActionNotice'
import Button from '../../shared/components/Button'
import PixelIcon from '../../shared/components/PixelIcon'
import SpotifyProgress from './SpotifyProgress'

/** What Spotify's redirect back to this page reports in `?spotify=`. */
const OUTCOMES: Record<string, { success?: string; error?: string }> = {
  connected: { success: 'Spotify connected.' },
  denied: { error: 'Spotify sign-in was cancelled.' },
  expired: { error: 'That Spotify sign-in expired. Try connecting again.' },
  failed: { error: 'Spotify could not be connected. Try again.' },
}

/** Plays through the student's own Spotify account inside this tab. Spotify requires Premium for this. */
export default function SpotifyPremiumPlayer({ status }: { status: SpotifyStatus }) {
  const [search, setSearch] = useSearchParams()
  const [outcome, setOutcome] = useState<{ success?: string; error?: string }>({})
  const connected = status.connected
  const player = useSpotifyPlayer(connected)
  const [pending, setPending] = useState(false)
  const [volume, setVolume] = useState(DEFAULT_VOLUME)
  const [volumeOpen, setVolumeOpen] = useState(false)

  // Show the sign-in outcome once, then drop it from the address so a refresh does not repeat it.
  useEffect(() => {
    const result = search.get('spotify')
    if (!result) return
    queueMicrotask(() => setOutcome(OUTCOMES[result] ?? {}))
    const rest = new URLSearchParams(search)
    rest.delete('spotify')
    setSearch(rest, { replace: true })
  }, [search, setSearch])

  async function connect() {
    setPending(true)
    setOutcome({})
    try {
      window.location.assign(await startSpotifyAuthorization())
    } catch (error) {
      setOutcome({ error: errorMessage(error) })
      setPending(false)
    }
  }

  if (!status.configured) {
    return <p className="muted">Spotify sign-in is not set up on this server. Switch to the free playlist above.</p>
  }

  if (!connected) {
    return (
      <div className="spotify-panel">
        <ActionNotice error={outcome.error} success={outcome.success} />
        <p>Connect your Spotify account to play your music while you study. Spotify only allows this with a Premium account; without one, switch to the free playlist above.</p>
        <div className="actions">
          <Button variant="primary" disabled={pending} onClick={() => void connect()}>Connect Spotify</Button>
        </div>
      </div>
    )
  }

  const nowPlaying = player.nowPlaying
  return (
    <div className="spotify-panel">
      <ActionNotice error={outcome.error ?? player.error} success={outcome.success} />
      {player.status === 'starting' && (
        <p className="row-meta" role="status">Starting the Spotify player…</p>
      )}
      {player.status === 'ready' && (
        <div className="spotify-now-playing">
          {nowPlaying?.imageUrl
            ? <img className="spotify-cover" src={nowPlaying.imageUrl} alt={`Cover of ${nowPlaying.name}`} width={200} height={200} />
            : <div className="spotify-cover spotify-cover-empty" aria-hidden="true"><PixelIcon kind="music" /></div>}
          <div className="spotify-track">
            <p><strong>{nowPlaying ? nowPlaying.name : 'Nothing playing'}</strong></p>
            <p className="row-meta">{nowPlaying ? nowPlaying.artists : 'In the Spotify app, pick "Study Buddy Matcher" as the device to play here.'}</p>
          </div>
          {nowPlaying && <SpotifyProgress track={nowPlaying} onSeek={player.seek} />}
          {/* Symbols keep the controls compact; the labels tell screen readers what each one does. */}
          <div className="spotify-controls">
            <Button disabled={!nowPlaying} onClick={player.previousTrack} aria-label="Previous track" title="Previous track">⏮</Button>
            <Button variant="primary" disabled={!nowPlaying} onClick={player.togglePlay}
              aria-label={nowPlaying?.paused === false ? 'Pause' : 'Play'} title={nowPlaying?.paused === false ? 'Pause' : 'Play'}>
              {nowPlaying?.paused === false ? '⏸' : '▶'}
            </Button>
            <Button disabled={!nowPlaying} onClick={player.nextTrack} aria-label="Next track" title="Next track">⏭</Button>
            <Button aria-expanded={volumeOpen} aria-controls="spotify-volume" onClick={() => setVolumeOpen(open => !open)}
              aria-label="Volume" title="Volume">{volume === 0 ? '🔇' : '🔊'}</Button>
          </div>
          {volumeOpen && <input id="spotify-volume" className="spotify-volume" type="range" aria-label="Spotify volume"
            min={0} max={1} step={0.05} value={volume}
            onChange={event => { const next = Number(event.target.value); setVolume(next); player.setVolume(next) }} />}
        </div>
      )}
    </div>
  )
}
