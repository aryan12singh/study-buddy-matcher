import type { AudioPreset } from './api'

/** Original synthesized audio; no tracks, accounts, provider requests or asset downloads. */
export class RoomAudioEngine {
  private context: AudioContext | null = null
  private layers = new Map<'music' | 'ambient', { source: AudioBufferSourceNode; gain: GainNode; preset: AudioPreset }>()

  async enable() {
    if (!this.context) {
      if (!window.AudioContext) throw new Error('Audio is unavailable in this browser. The room works silently.')
      this.context = new AudioContext()
    }
    await this.context.resume()
  }

  play(layer: 'music' | 'ambient', preset: AudioPreset, volume: number) {
    if (!this.context || this.context.state !== 'running') throw new Error('Enable audio in this browser first.')
    const current = this.layers.get(layer)
    if (current?.preset === preset) { current.gain.gain.value = Math.min(1, Math.max(0, volume)); return }
    this.stop(layer)
    const context = this.context
    const gain = context.createGain()
    gain.gain.value = Math.min(1, Math.max(0, volume))
    gain.connect(context.destination)
    const duration = preset === 'CALM_MUSIC' ? 16 : 4
    const buffer = context.createBuffer(1, Math.ceil(context.sampleRate * duration), context.sampleRate)
    const samples = buffer.getChannelData(0)
    // A simple original pentatonic melody, with a soft envelope on each note.
    const notes = [261.63, 329.63, 392, 440, 392, 329.63, 293.66, 261.63]
    let smooth = 0
    for (let i = 0; i < samples.length; i++) {
      const time = i / context.sampleRate
      const noise = Math.random() * 2 - 1
      smooth = smooth * 0.985 + noise * 0.015
      if (preset === 'CALM_MUSIC') {
        const noteTime = time % 2
        const envelope = Math.sin(Math.PI * noteTime / 2) ** 2
        const frequency = notes[Math.floor(time / 2) % notes.length]
        samples[i] = envelope * (Math.sin(2 * Math.PI * frequency * time) * 0.13
          + Math.sin(2 * Math.PI * frequency * 2 * time) * 0.035)
      } else if (preset === 'RAIN') samples[i] = noise * 0.075 + smooth * 0.3
      else if (preset === 'CAFE') samples[i] = smooth * 0.75 + noise * 0.025
      else samples[i] = noise * 0.075
    }
    const source = context.createBufferSource()
    source.buffer = buffer; source.loop = true; source.connect(gain); source.start()
    this.layers.set(layer, { source, gain, preset })
  }

  stop(layer?: 'music' | 'ambient') {
    for (const [key, audio] of this.layers) {
      if (layer && key !== layer) continue
      audio.source.stop(); audio.source.disconnect(); audio.gain.disconnect(); this.layers.delete(key)
    }
  }
  close() { this.stop(); const context = this.context; this.context = null; void context?.close().catch(() => {}) }
}
