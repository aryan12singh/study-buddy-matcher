import { useEffect, useRef, useState } from 'react'
import type { AudioPreset, StudyRoom } from './api'
import { RoomAudioEngine } from './RoomAudioEngine'
import Button from '../../shared/components/Button'
import Field from '../../shared/components/Field'

export default function RoomAudioPanel({ room, active, pending, onSelect }: {
  room: StudyRoom; active: boolean; pending: boolean; onSelect: (preset: AudioPreset, playing: boolean) => void
}) {
  const engine = useRef<RoomAudioEngine | null>(null)
  const [enabled, setEnabled] = useState(false)
  const [volume, setVolume] = useState(25)
  const [muted, setMuted] = useState(false)
  const [error, setError] = useState<string>()
  const canControl = room.canControl && (active || room.leader)
  useEffect(() => {
    const audio = new RoomAudioEngine(); engine.current = audio
    return () => { audio.close(); if (engine.current === audio) engine.current = null }
  }, [])
  useEffect(() => {
    const audio = engine.current
    let disposed = false
    if (!active || !enabled || !room.audio.playing) { audio?.stop(); return }
    try { audio?.play(room.audio.preset, muted ? 0 : volume / 100) }
    catch (failure) {
      audio?.close()
      queueMicrotask(() => {
        if (!disposed) {
          setEnabled(false)
          setError(failure instanceof Error ? failure.message : 'Audio unavailable; the room works silently.')
        }
      })
    }
    return () => { disposed = true }
  }, [active, enabled, room.audio.playing, room.audio.preset, volume, muted])

  async function enable() {
    const audio = engine.current
    if (!audio) return
    try { await audio.enable(); if (engine.current === audio) { setError(undefined); setEnabled(true) } }
    catch (failure) { if (engine.current === audio) setError(failure instanceof Error ? failure.message : 'Audio unavailable; the room works silently.') }
  }
  return <section className="detail-panel">
    <h2>Room audio</h2>
    <p>Original music and ambient presets. Everyone shares the selection; volume and mute stay in your browser.</p>
    <Field id="room-audio-preset" label="Shared selection">
      <select id="room-audio-preset" value={room.audio.preset} disabled={!canControl || pending}
        onChange={event => onSelect(event.target.value as AudioPreset, room.audio.playing)}>
        {room.audioPresets.map(preset => <option key={preset.id} value={preset.id}>{preset.label}</option>)}
      </select>
    </Field>
    <p role="status">{room.audio.playing ? 'Room audio is playing' : 'Room audio is paused'}</p>
    <div className="actions">
      {!enabled && <Button disabled={!active} onClick={() => void enable()}>Enable audio in this browser</Button>}
      {canControl && <Button disabled={pending} onClick={() => onSelect(room.audio.preset, !room.audio.playing)}>
        {room.audio.playing ? 'Pause room audio' : 'Play room audio'}
      </Button>}
    </div>
    <Field id="room-volume" label={`Your volume: ${volume}%`}>
      <input id="room-volume" type="range" min="0" max="100" value={volume} disabled={!enabled}
        onChange={event => setVolume(Number(event.target.value))} />
    </Field>
    <label className="checkbox-label"><input type="checkbox" checked={muted} disabled={!enabled}
      onChange={event => setMuted(event.target.checked)} /> Mute only my audio</label>
    {!enabled && <p className="muted">Audio stays silent until you enable it. The room works without audio.</p>}
    {enabled && !room.audio.playing && <p className="muted">Audio is enabled locally. A host can start the shared selection.</p>}
    {error && <p role="alert" className="field-error">{error}</p>}
  </section>
}
