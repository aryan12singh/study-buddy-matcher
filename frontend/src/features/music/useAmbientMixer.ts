import { useCallback, useEffect, useRef, useState } from 'react'
import { getAmbientSound } from './api'
import type { AmbientSound } from './api'
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

/**
 * One looping audio element per category, so sounds can be layered. Everything stops when the
 * page is left.
 */
export function useAmbientMixer() {
  const audios = useRef(new Map<string, HTMLAudioElement>())
  // Only the latest request per category may update it, so a slow earlier reply cannot win.
  const requests = useRef(new Map<string, number>())
  const mounted = useRef(true)
  const mutedRef = useRef(false)
  const [channels, setChannels] = useState<Record<string, Channel>>({})
  const [muted, setMuted] = useState(false)

  useEffect(() => {
    mounted.current = true
    const playing = audios.current
    return () => {
      mounted.current = false
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
    void load(id, channel(id).sound?.id)
  }

  function setVolume(id: string, volume: number) {
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
      startRequest(id)
      audio.pause()
      audio.currentTime = 0
    })
    setChannels(previous => Object.fromEntries(Object.entries(previous).map(([id, current]) =>
      [id, current.status === 'playing' || current.status === 'loading' ? { ...current, status: current.sound ? 'paused' : 'idle' } : current])))
  }

  const anyPlaying = Object.values(channels).some(current => current.status === 'playing')

  return { channel, muted, anyPlaying, toggle, newSound, setVolume, toggleMute, stopAll }
}
