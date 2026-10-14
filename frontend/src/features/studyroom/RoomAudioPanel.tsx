import { useEffect, useRef, useState } from 'react'
import type { RoomAudio, StudyRoom } from './api'
import { RoomAudioEngine } from './RoomAudioEngine'
import Button from '../../shared/components/Button'
import Field from '../../shared/components/Field'

export default function RoomAudioPanel({ room, active, pending, onSelect }: {
  room: StudyRoom; active: boolean; pending: boolean; onSelect: (audio: RoomAudio) => void
}) {
  const engine = useRef<RoomAudioEngine | null>(null)
  const [enabled, setEnabled] = useState(false)
  const [volume, setVolume] = useState({ music: 25, ambient: 25 })
  const [muted, setMuted] = useState({ music: false, ambient: false })
  const [error, setError] = useState<string>()
  const canControl = room.canControl && (active || room.leader)
  useEffect(() => {
    const audio = new RoomAudioEngine(); engine.current = audio
    return () => { audio.close(); if (engine.current === audio) engine.current = null }
  }, [])
  useEffect(() => {
    const audio = engine.current
    let disposed = false
    if (!active || !enabled) { audio?.stop(); return }
    try {
      if (room.audio.musicPlaying && room.audio.musicPreset) audio?.play('music', room.audio.musicPreset, muted.music ? 0 : volume.music / 100)
      else audio?.stop('music')
      if (room.audio.ambientPlaying && room.audio.ambientPreset) audio?.play('ambient', room.audio.ambientPreset, muted.ambient ? 0 : volume.ambient / 100)
      else audio?.stop('ambient')
    } catch (failure) {
      audio?.close()
      queueMicrotask(() => {
        if (!disposed) {
          setEnabled(false)
          setError(failure instanceof Error ? failure.message : 'Audio unavailable; the room works silently.')
        }
      })
    }
    return () => { disposed = true }
  }, [active, enabled, room.audio, volume, muted])

  async function enable() {
    const audio = engine.current
    if (!audio) return
    try { await audio.enable(); if (engine.current === audio) { setError(undefined); setEnabled(true) } }
    catch (failure) { if (engine.current === audio) setError(failure instanceof Error ? failure.message : 'Audio unavailable; the room works silently.') }
  }
  return <section className="detail-panel">
    <h2>Room audio</h2>
    <p>Mix original music with ambience. Everyone shares each selection and play state; volume and mute stay in your browser.</p>
    <div className="form-grid">
      {(['music', 'ambient'] as const).map(layer => {
        const title = layer === 'music' ? 'Music' : 'Ambience'
        const preset = layer === 'music' ? room.audio.musicPreset : room.audio.ambientPreset
        const playing = layer === 'music' ? room.audio.musicPlaying : room.audio.ambientPlaying
        const choices = room.audioPresets.filter(choice => choice.kind === (layer === 'music' ? 'MUSIC' : 'AMBIENT'))
        return <fieldset key={layer} className="fieldset">
          <legend>{title}</legend>
          <Field id={`room-${layer}-preset`} label={`Shared ${title.toLowerCase()} selection`}>
            <select id={`room-${layer}-preset`} value={preset ?? ''} disabled={!canControl || pending}
              onChange={event => {
                const next = event.target.value || null
                onSelect(layer === 'music' ? { ...room.audio, musicPreset: next as RoomAudio['musicPreset'], musicPlaying: !!next && playing }
                  : { ...room.audio, ambientPreset: next as RoomAudio['ambientPreset'], ambientPlaying: !!next && playing })
              }}>
              <option value="">Off</option>
              {choices.map(choice => <option key={choice.id} value={choice.id}>{choice.label}</option>)}
            </select>
          </Field>
          <p role="status">{title} is {playing ? 'playing' : 'paused'}</p>
          {canControl && <Button disabled={pending || !preset} onClick={() => onSelect(layer === 'music'
            ? { ...room.audio, musicPlaying: !playing } : { ...room.audio, ambientPlaying: !playing })}>
            {playing ? `Pause ${title.toLowerCase()} for everyone` : `Play ${title.toLowerCase()} for everyone`}
          </Button>}
          <Field id={`room-${layer}-volume`} label={`Your ${title.toLowerCase()} volume: ${volume[layer]}%`}>
            <input id={`room-${layer}-volume`} type="range" min="0" max="100" value={volume[layer]} disabled={!enabled}
              onChange={event => setVolume({ ...volume, [layer]: Number(event.target.value) })} />
          </Field>
          <label className="checkbox-label"><input type="checkbox" checked={muted[layer]} disabled={!enabled}
            onChange={event => setMuted({ ...muted, [layer]: event.target.checked })} /> Mute only my {title.toLowerCase()}</label>
        </fieldset>
      })}
    </div>
    {!enabled && <Button disabled={!active} onClick={() => void enable()}>Enable audio in this browser</Button>}
    {!enabled && <p className="muted">Audio stays silent until you enable it. The room works without audio.</p>}
    {error && <p role="alert" className="field-error">{error}</p>}
  </section>
}
