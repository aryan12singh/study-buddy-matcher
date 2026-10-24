import { useCallback, useEffect, useRef, useState } from 'react'
import { getAmbientSound } from './api'
import type { AmbientSound } from './api'
import { planRemix } from './remixPlan'
import { errorMessage } from '../../shared/api/client'

export type ChannelStatus = 'idle' | 'loading' | 'playing' | 'paused' | 'error'

export type Channel = {
  status: ChannelStatus
  volume: number
  sound?: AmbientSound
  error?: string
}

/** Below full volume so several layered sounds do not start out too loud. */
export const DEFAULT_VOLUME = 0.6
const IDLE: Channel = { status: 'idle', volume: DEFAULT_VOLUME }
const PLAYBACK_ERROR = 'Your browser could not play this sound. Try another sound.'
/** Volume changes made by a remix glide over this long, in this many steps. */
const FADE_MS = 3000
const FADE_STEPS = 30

/**
 * One looping audio element per category, so sounds can be layered. Everything stops when the
 * page is left.
 */
export function useAmbientMixer() {
  const audios = useRef(new Map<string, HTMLAudioElement>())
  // Only the latest request per category may update it, so a slow earlier reply cannot win.
  const requests = useRef(new Map<string, number>())
  const fades = useRef(new Map<string, number>())
  const mounted = useRef(true)
  const mutedRef = useRef(false)
  const [channels, setChannels] = useState<Record<string, Channel>>({})
  const [muted, setMuted] = useState(false)

  useEffect(() => {
    mounted.current = true
    const playing = audios.current
    const fading = fades.current
    return () => {
      mounted.current = false
      fading.forEach(timer => window.clearInterval(timer))
      fading.clear()
      playing.forEach(audio => {
        audio.pause()
        audio.removeAttribute('src')
        audio.load()
      })
      playing.clear()
    }
  }, [])

  const update = useCallback((id: string, change: Partial<Channel>) => {
    setChannels(previous => ({ ...previous, [id]: { ...(previous[id] ?? IDLE), ...change } }))
  }, [])

  function audioFor(id: string) {
    let audio = audios.current.get(id)
    if (!audio) {
      audio = new Audio()
      audio.loop = true
      audio.volume = DEFAULT_VOLUME
      audio.muted = mutedRef.current
      audios.current.set(id, audio)
    }
    return audio
  }

  function cancelFade(id: string) {
    const timer = fades.current.get(id)
    if (timer === undefined) return
    window.clearInterval(timer)
    fades.current.delete(id)
  }

  /** Stops any remix fade and puts the sound back at the volume its slider shows. */
  function settle(id: string) {
    cancelFade(id)
    audioFor(id).volume = channel(id).volume
  }

  function fade(id: string, to: number, done?: () => void) {
    cancelFade(id)
    const audio = audioFor(id)
    const from = audio.volume
    let step = 0
    fades.current.set(id, window.setInterval(() => {
      step++
      audio.volume = from + (to - from) * Math.min(1, step / FADE_STEPS)
      if (step < FADE_STEPS) return
      cancelFade(id)
      done?.()
    }, FADE_MS / FADE_STEPS))
  }

  function startRequest(id: string) {
    const request = (requests.current.get(id) ?? 0) + 1
    requests.current.set(id, request)
    return () => mounted.current && requests.current.get(id) === request
  }

  async function play(id: string, isCurrent: () => boolean) {
    try {
      await audioFor(id).play()
      if (isCurrent()) update(id, { status: 'playing', error: undefined })
    } catch {
      if (isCurrent()) update(id, { status: 'error', error: PLAYBACK_ERROR })
    }
  }

  async function load(id: string, exclude?: number) {
    const isCurrent = startRequest(id)
    update(id, { status: 'loading', error: undefined })
    try {
      const sound = await getAmbientSound(id, exclude)
      if (!isCurrent()) return
      audioFor(id).src = sound.previewUrl
      update(id, { sound })
      await play(id, isCurrent)
    } catch (error) {
      if (isCurrent()) update(id, { status: 'error', error: errorMessage(error) })
    }
  }

  function channel(id: string) {
    return channels[id] ?? IDLE
  }

  function toggle(id: string) {
    const current = channel(id)
    settle(id)
    if (current.status === 'playing') {
      startRequest(id)
      audioFor(id).pause()
      update(id, { status: 'paused' })
    } else if (current.sound) {
      update(id, { status: 'loading', error: undefined })
      void play(id, startRequest(id))
    } else {
      void load(id)
    }
  }

  function newSound(id: string) {
    settle(id)
    void load(id, channel(id).sound?.id)
  }

  function setVolume(id: string, volume: number) {
    cancelFade(id)
    audioFor(id).volume = volume
    update(id, { volume })
  }

  function toggleMute() {
    const next = !mutedRef.current
    mutedRef.current = next
    audios.current.forEach(audio => { audio.muted = next })
    setMuted(next)
  }

  function stopAll() {
    audios.current.forEach((audio, id) => {
      settle(id)
      startRequest(id)
      audio.pause()
      audio.currentTime = 0
    })
    setChannels(previous => Object.fromEntries(Object.entries(previous).map(([id, current]) =>
      [id, current.status === 'playing' || current.status === 'loading' ? { ...current, status: current.sound ? 'paused' : 'idle' } : current])))
  }

  function fadeOut(id: string) {
    const current = channel(id)
    if (current.status !== 'playing' && current.status !== 'loading') return
    const isCurrent = startRequest(id)
    fade(id, 0, () => {
      const audio = audioFor(id)
      audio.pause()
      audio.volume = current.volume
      if (isCurrent()) update(id, { status: current.sound ? 'paused' : 'idle' })
    })
  }

  /** Plays a random few of these categories at random volumes, fading the rest out. */
  function remix(ids: string[]) {
    const picks = planRemix(ids, id => channel(id).status === 'playing')
    ids.filter(id => !picks.some(pick => pick.id === id)).forEach(fadeOut)
    picks.forEach(({ id, volume, newSound: swap }) => {
      const current = channel(id)
      update(id, { volume })
      if (!swap) {
        fade(id, volume)
        return
      }
      // New or swapped sounds fade in from silence.
      cancelFade(id)
      audioFor(id).volume = 0
      fade(id, volume)
      if (current.sound && current.status !== 'playing' && current.status !== 'loading') {
        update(id, { status: 'loading', error: undefined })
        void play(id, startRequest(id))
      } else {
        void load(id, current.sound?.id)
      }
    })
  }

  const anyPlaying = Object.values(channels).some(current => current.status === 'playing')

  return { channel, muted, anyPlaying, toggle, newSound, setVolume, toggleMute, stopAll, remix }
}
