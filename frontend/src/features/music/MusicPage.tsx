import { getAmbientCategories } from './api'
import { useAmbientMixer } from './useAmbientMixer'
import AmbientChannel from './AmbientChannel'
import SpotifyPanel from './SpotifyPanel'
import { useResource } from '../../shared/api/useResource'
import WindowPage from '../../shared/components/WindowPage'
import StatePanel from '../../shared/components/StatePanel'
import Button from '../../shared/components/Button'
import './music.css'

export default function MusicPage() {
  const categories = useResource('ambient-categories', getAmbientCategories, true, false)
  const mixer = useAmbientMixer()
  return (
    <WindowPage
      title="Music"
      description="Play your Spotify music and layer ambient sounds to study to. Everything stops when you leave this page."
    >
      <section className="music-section" aria-labelledby="spotify-heading">
        <h2 id="spotify-heading">Spotify</h2>
        <SpotifyPanel />
      </section>
      <section className="music-section" aria-labelledby="ambient-heading">
        <h2 id="ambient-heading">Ambient sounds</h2>
        <div className="actions">
          <Button aria-pressed={mixer.muted} onClick={mixer.toggleMute}>{mixer.muted ? 'Unmute all sounds' : 'Mute all sounds'}</Button>
          <Button disabled={!mixer.anyPlaying} onClick={mixer.stopAll}>Stop all sounds</Button>
        </div>
        {categories.loading ? (
          <StatePanel loading />
        ) : categories.error ? (
          <StatePanel error={categories.error} onRetry={() => categories.reload(true)} />
        ) : !categories.data?.length ? (
          <StatePanel empty emptyKind="music" emptyTitle="No ambient sounds yet" emptyMessage="No sound categories are set up on this server." />
        ) : (
          <>
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
            <p className="row-meta">Sounds from <a href="https://freesound.org/" target="_blank" rel="noreferrer">Freesound</a>; each one links to its own licence.</p>
          </>
        )}
      </section>
    </WindowPage>
  )
}
