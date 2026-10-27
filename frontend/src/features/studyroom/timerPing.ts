import { useEffect, useRef } from 'react'
import type { TimerPhase, TimerStatus } from './api'

/** Quiet enough to sit under music, loud enough to notice. */
const PING_VOLUME = 0.18
const NOTE_GAP_SECONDS = 0.16
const NOTE_LENGTH_SECONDS = 0.5
/** A rising pair to start, a falling pair to finish. */
const NOTES = { start: [660, 880], end: [880, 587] } as const

export type PingKind = keyof typeof NOTES

let context: AudioContext | undefined

/** Plays a short two-note chime, synthesised in the browser, so no sound file is shipped. */
export function playPing(kind: PingKind) {
  try {
    context ??= new AudioContext()
    if (context.state === 'suspended') void context.resume()
    const audio = context
    NOTES[kind].forEach((frequency, index) => {
      const at = audio.currentTime + index * NOTE_GAP_SECONDS
      const tone = audio.createOscillator()
      const gain = audio.createGain()
      tone.type = 'sine'
      tone.frequency.value = frequency
      gain.gain.setValueAtTime(0.0001, at)
      gain.gain.exponentialRampToValueAtTime(PING_VOLUME, at + 0.01)
      gain.gain.exponentialRampToValueAtTime(0.0001, at + NOTE_LENGTH_SECONDS)
      tone.connect(gain).connect(audio.destination)
      tone.start(at)
      tone.stop(at + NOTE_LENGTH_SECONDS + 0.05)
    })
  } catch {
    // The chime is a nicety; the timer works silently where the browser has no audio.
  }
}

/**
 * Chimes when a session starts or ends. Given a phase, it also chimes when focus gives way to a
 * break (end) and a break gives way to focus (start). The first state seen never chimes, so
 * opening a page mid-session stays quiet.
 */
export function useTimerPings(status: TimerStatus, phase?: TimerPhase, enabled = true) {
  const previous = useRef<{ status: TimerStatus; phase?: TimerPhase } | null>(null)
  useEffect(() => {
    const before = previous.current
    previous.current = { status, phase }
    if (!before || !enabled) return
    if (before.status === 'IDLE' && status === 'RUNNING') playPing('start')
    else if (before.status !== 'IDLE' && status === 'IDLE') playPing('end')
    else if (phase && status === 'RUNNING' && before.phase !== phase) playPing(phase === 'FOCUS' ? 'start' : 'end')
  }, [status, phase, enabled])
}
