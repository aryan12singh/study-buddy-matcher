import { getAmbientCategories } from './api'
import { useAmbientMixer } from './useAmbientMixer'
import AmbientChannel from './AmbientChannel'
import SpotifyPanel from './SpotifyPanel'
import { useResource } from '../../shared/api/useResource'
import StatePanel from '../../shared/components/StatePanel'
import Button from '../../shared/components/Button'
import './music.css'

/** Spotify playback and the ambient mixer for the study rooms. */
export default function MusicPlayer() {
  const categories = useResource('ambient-categories', getAmbientCategories, true, false)
  const mixer = useAmbientMixer()
  return (
    <div className="music-player">
      <section className="music-section" aria-labelledby="spotify-heading">
        <h2 id="spotify-heading">Spotify</h2>
        <SpotifyPanel />
      </section>
      <section className="music-section" aria-labelledby="ambient-heading">
        <h2 id="ambient-heading">Ambient sounds</h2>
        <div className="actions">
          <Button aria-pressed={mixer.muted} onClick={mixer.toggleMute}>{mixer.muted ? 'Unmute all sounds' : 'Mute all sounds'}</Button>
          <Button disabled={!mixer.anyPlaying} onClick={mixer.stopAll}>Stop all sounds</Button>
          <Button variant="primary" disabled={!categories.data?.length}
            onClick={() => mixer.remix(categories.data?.map(category => category.id) ?? [])}>Remix sounds</Button>
        </div>
        {categories.loading ? (
          <StatePanel loading />
        ) : categories.error ? (
          <StatePanel error={categories.error} onRetry={() => categories.reload(true)} />
        ) : !categories.data?.length ? (
          <StatePanel empty emptyKind="music" emptyTitle="No ambient sounds yet" emptyMessage="No sound categories are set up on this server." />
        ) : (
          <div className="data-list">
            {categories.data.map(category => (
              <AmbientChannel
                key={category.id}
                category={category}
                channel={mixer.channel(category.id)}
                onToggle={() => mixer.toggle(category.id)}
                onNewSound={() => mixer.newSound(category.id)}
                onVolume={volume => mixer.setVolume(category.id, volume)}
              />
            ))}
          </div>
        )}
      </section>
    </div>
  )
}
