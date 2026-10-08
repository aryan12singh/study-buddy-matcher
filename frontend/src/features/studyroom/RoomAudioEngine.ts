import type { AudioPreset } from './api'

/** Original synthesized audio; no tracks, accounts, provider requests or asset downloads. */
export class RoomAudioEngine {
  private context: AudioContext | null = null
  private source: AudioBufferSourceNode | null = null
  private gain: GainNode | null = null
  private preset: AudioPreset | null = null

  async enable() {
    if (!this.context) {
      if (!window.AudioContext) throw new Error('Audio is unavailable in this browser. The room works silently.')
      this.context = new AudioContext()
      this.gain = this.context.createGain()
      this.gain.connect(this.context.destination)
    }
    await this.context.resume()
  }

  play(preset: AudioPreset, volume: number) {
    if (!this.context || !this.gain || this.context.state !== 'running') throw new Error('Enable audio in this browser first.')
    this.gain.gain.value = Math.min(1, Math.max(0, volume))
    if (this.source && this.preset === preset) return
    this.stop()
    const context = this.context
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
    source.buffer = buffer; source.loop = true; source.connect(this.gain); source.start()
    this.source = source; this.preset = preset
  }

  stop() { this.source?.stop(); this.source?.disconnect(); this.source = null; this.preset = null }
  close() { this.stop(); const context = this.context; this.context = null; this.gain = null; void context?.close().catch(() => {}) }
}
