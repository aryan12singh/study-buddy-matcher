import { useEffect } from 'react'
import { getAmbientCategories } from './api'
import { useMusicSettings } from './musicSettingsContext'
import { useAmbient } from './ambientContext'
import AmbientChannel from './AmbientChannel'
import SpotifyPanel from './SpotifyPanel'
import { useResource } from '../../shared/api/useResource'
import StatePanel from '../../shared/components/StatePanel'
import Button from '../../shared/components/Button'
import PixelIcon from '../../shared/components/PixelIcon'
import './music.css'

/** Spotify playback and the ambient mixer, shown in the Music tab of each study room's settings. */
export default function MusicPlayer() {
  const categories = useResource('ambient-categories', getAmbientCategories, true, false)
  const mixer = useAmbient()
  const { markOpened } = useMusicSettings()
  // The tab mounts this the first time it is shown, which tells the corner prompt music has been chosen.
  useEffect(() => { markOpened() }, [markOpened])
  return (
    <div className="music-player">
      <section className="music-card" aria-labelledby="spotify-heading">
        <h2 id="spotify-heading" className="music-card-title"><PixelIcon kind="music" />Spotify</h2>
        <SpotifyPanel />
      </section>
      <section className="music-card" aria-labelledby="ambient-heading">
        <h2 id="ambient-heading" className="music-card-title"><PixelIcon kind="brand" />Ambient sounds</h2>
        <div className="music-toolbar">
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
          <div className="ambient-grid">
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
