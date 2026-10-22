import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { useSearchParams } from 'react-router-dom'
import { disconnectSpotify, getSpotifyStatus, playSpotifyPlaylist, searchSpotifyPlaylists, startSpotifyAuthorization } from './spotifyApi'
import type { SpotifyPlaylist } from './spotifyApi'
import { useSpotifyPlayer } from './useSpotifyPlayer'
import { errorMessage } from '../../shared/api/client'
import { useResource } from '../../shared/api/useResource'
import StatePanel from '../../shared/components/StatePanel'
import ActionNotice from '../../shared/components/ActionNotice'
import Button from '../../shared/components/Button'
import Field from '../../shared/components/Field'

const MAX_QUERY_LENGTH = 100

/** What Spotify's redirect back to this page reports in `?spotify=`. */
const OUTCOMES: Record<string, { success?: string; error?: string }> = {
  connected: { success: 'Spotify connected.' },
  denied: { error: 'Spotify sign-in was cancelled.' },
  expired: { error: 'That Spotify sign-in expired. Try connecting again.' },
  failed: { error: 'Spotify could not be connected. Try again.' },
}

export default function SpotifyPanel() {
  const [search, setSearch] = useSearchParams()
  const [outcome, setOutcome] = useState<{ success?: string; error?: string }>({})
  const status = useResource('spotify-status', getSpotifyStatus, true, false)
  const connected = Boolean(status.data?.connected)
  const player = useSpotifyPlayer(connected)
  const [draft, setDraft] = useState('')
  const [draftError, setDraftError] = useState<string>()
  const [query, setQuery] = useState('')
  const playlists = useResource(`spotify-playlists-${query}`, signal => searchSpotifyPlaylists(query, signal), connected && query !== '', false)
  const [pending, setPending] = useState(false)

  // Show the sign-in outcome once, then drop it from the address so a refresh does not repeat it.
  useEffect(() => {
    const result = search.get('spotify')
    if (!result) return
    queueMicrotask(() => setOutcome(OUTCOMES[result] ?? {}))
    const rest = new URLSearchParams(search)
    rest.delete('spotify')
    setSearch(rest, { replace: true })
  }, [search, setSearch])

  async function run(action: () => Promise<void>, success?: string) {
    setPending(true)
    setOutcome({})
    try {
      await action()
      if (success) setOutcome({ success })
    } catch (error) {
      setOutcome({ error: errorMessage(error) })
    } finally {
      setPending(false)
    }
  }

  function connect() {
    void run(async () => { window.location.assign(await startSpotifyAuthorization()) })
  }

  function disconnect() {
    void run(async () => {
      await disconnectSpotify()
      status.reload(true)
    }, 'Spotify disconnected.')
  }

  function play(playlist: SpotifyPlaylist) {
    if (!player.deviceId) return
    const deviceId = player.deviceId
    void run(async () => {
      await player.activate()
      await playSpotifyPlaylist(deviceId, playlist.uri)
    })
  }

  function submit(event: FormEvent) {
    event.preventDefault()
    const trimmed = draft.trim()
    if (!trimmed) return setDraftError('Enter something to search for, such as "lofi" or "focus".')
    if (trimmed.length > MAX_QUERY_LENGTH) return setDraftError(`Search must be at most ${MAX_QUERY_LENGTH} characters.`)
    setDraftError(undefined)
    setQuery(trimmed)
  }

  if (status.loading) return <StatePanel loading />
  if (status.error) return <StatePanel error={status.error} onRetry={() => status.reload(true)} />

  if (!status.data?.configured) {
    return <p className="muted">Spotify is not set up on this server.</p>
  }

  if (!connected) {
    return (
      <div className="spotify-panel">
        <ActionNotice error={outcome.error} success={outcome.success} />
        <p>Connect your Spotify account to play your music while you study. Spotify only allows this with a Premium account.</p>
        <div className="actions">
          <Button variant="primary" disabled={pending} onClick={connect}>Connect Spotify</Button>
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
          {nowPlaying?.imageUrl && (
            <img src={nowPlaying.imageUrl} alt="" width={64} height={64} />
          )}
          <div>
            <p><strong>{nowPlaying ? nowPlaying.name : 'Nothing playing'}</strong></p>
            <p className="row-meta">{nowPlaying ? nowPlaying.artists : 'Search for a playlist below, or pick "Study Buddy Matcher" as the device in the Spotify app.'}</p>
            <div className="actions">
              <Button disabled={!nowPlaying} onClick={player.previousTrack}>Previous track</Button>
              <Button variant="primary" disabled={!nowPlaying} onClick={player.togglePlay}>{nowPlaying?.paused === false ? 'Pause' : 'Resume'}</Button>
              <Button disabled={!nowPlaying} onClick={player.nextTrack}>Next track</Button>
            </div>
            <Field label="Spotify volume" id="spotify-volume">
              <input
                id="spotify-volume"
                type="range"
                min={0}
                max={1}
                step={0.05}
                defaultValue={0.5}
                onChange={event => player.setVolume(Number(event.target.value))}
              />
            </Field>
          </div>
        </div>
      )}
      <form className="stack-form" onSubmit={submit} noValidate>
        <Field label="Search Spotify playlists" id="spotify-query" error={draftError}>
          <input
            id="spotify-query"
            type="search"
            value={draft}
            maxLength={MAX_QUERY_LENGTH}
            onChange={event => setDraft(event.target.value)}
          />
        </Field>
        <div className="actions">
          <Button type="submit" variant="primary" disabled={playlists.loading}>Search playlists</Button>
          <Button disabled={pending} onClick={disconnect}>Disconnect Spotify</Button>
        </div>
      </form>
      {query !== '' && (playlists.loading ? (
        <StatePanel loading />
      ) : playlists.error ? (
        <StatePanel error={playlists.error} onRetry={() => playlists.reload(true)} />
      ) : !playlists.data?.length ? (
        <StatePanel empty emptyKind="music" emptyTitle="No playlists found" emptyMessage={`Nothing matched "${query}". Try a different word.`} />
      ) : (
        <div className="data-list">
          {playlists.data.map(playlist => (
            <article key={playlist.id} className="data-row spotify-playlist">
              {playlist.imageUrl && (
                <img src={playlist.imageUrl} alt="" width={56} height={56} />
              )}
              <div>
                <h3>{playlist.name}</h3>
                <p className="row-meta">By {playlist.owner || 'Spotify user'}</p>
              </div>
              <div className="actions">
                <Button variant="primary" disabled={pending || !player.deviceId} onClick={() => play(playlist)}>Play playlist</Button>
              </div>
            </article>
          ))}
        </div>
      ))}
    </div>
  )
}
