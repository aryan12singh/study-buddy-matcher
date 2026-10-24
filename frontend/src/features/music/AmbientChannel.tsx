import type { AmbientCategory } from './api'
import type { Channel } from './useAmbientMixer'
import Button from '../../shared/components/Button'
import MusicSlider from './MusicSlider'

export default function AmbientChannel({ category, channel, onToggle, onNewSound, onVolume }: {
  category: AmbientCategory
  channel: Channel
  onToggle: () => void
  onNewSound: () => void
  onVolume: (volume: number) => void
}) {
  const loading = channel.status === 'loading'
  const playing = channel.status === 'playing'
  const name = category.label.toLowerCase()
  const volumeId = `ambient-volume-${category.id}`
  return (
    <article className={`ambient-tile${playing ? ' is-playing' : ''}`} aria-busy={loading}>
      <div className="ambient-tile-head">
        <h3>
          {/* Decorative bars that bounce while the sound plays. */}
          <span className="ambient-bars" aria-hidden="true"><i /><i /><i /></span>
          {category.label}
        </h3>
        <Button variant={playing ? 'secondary' : 'primary'} disabled={loading} aria-pressed={playing} onClick={onToggle}>
          {playing ? `Pause ${name}` : `Play ${name}`}
        </Button>
      </div>
      {loading ? (
        <p className="ambient-status" role="status">Finding a {name} sound…</p>
      ) : channel.sound ? (
        <p className="ambient-status">{playing ? 'Playing' : 'Paused'}</p>
      ) : (
        <p className="ambient-status">Not playing</p>
      )}
      {channel.error && <p className="field-error" role="alert">{channel.error}</p>}
      <div className="ambient-tile-foot">
        <label className="ambient-volume" htmlFor={volumeId}>
          <span className="visually-hidden">{category.label} volume</span>
          <span aria-hidden="true">Vol</span>
          <MusicSlider id={volumeId} min={0} max={1} step={0.05} value={channel.volume} onValue={onVolume} />
        </label>
        <Button className="ambient-another" disabled={loading || !channel.sound} onClick={onNewSound}>Try another sound</Button>
      </div>
    </article>
  )
}
