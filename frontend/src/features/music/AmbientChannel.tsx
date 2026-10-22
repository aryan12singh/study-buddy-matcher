import type { AmbientCategory } from './api'
import type { Channel } from './useAmbientMixer'
import Button from '../../shared/components/Button'
import Field from '../../shared/components/Field'

export default function AmbientChannel({ category, channel, onToggle, onNewSound, onVolume }: {
  category: AmbientCategory
  channel: Channel
  onToggle: () => void
  onNewSound: () => void
  onVolume: (volume: number) => void
}) {
  const loading = channel.status === 'loading'
  const playing = channel.status === 'playing'
  const volumeId = `ambient-volume-${category.id}`
  return (
    <article className="data-row" aria-busy={loading}>
      <div>
        <h3>{category.label}</h3>
        {loading ? (
          <p className="row-meta" role="status">Finding a {category.label.toLowerCase()} sound…</p>
        ) : channel.sound ? (
          <p className="row-meta">
            {playing ? 'Playing' : 'Paused'}: {channel.sound.name} by {channel.sound.author} · <a href={channel.sound.license} target="_blank" rel="noreferrer">Licence</a>
          </p>
        ) : (
          <p className="row-meta">Not playing</p>
        )}
        {channel.error && (
          <p className="field-error" role="alert">{channel.error}</p>
        )}
        <Field label={`${category.label} volume`} id={volumeId}>
          <input
            id={volumeId}
            type="range"
            min={0}
            max={1}
            step={0.05}
            value={channel.volume}
            onChange={event => onVolume(Number(event.target.value))}
          />
        </Field>
      </div>
      <div className="actions">
        <Button variant={playing ? 'secondary' : 'primary'} disabled={loading} aria-pressed={playing} onClick={onToggle}>
          {playing ? `Pause ${category.label.toLowerCase()}` : `Play ${category.label.toLowerCase()}`}
        </Button>
        <Button disabled={loading || !channel.sound} onClick={onNewSound}>Try another sound</Button>
      </div>
    </article>
  )
}
