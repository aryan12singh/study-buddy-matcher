import { createContext, useContext } from 'react'
import type { useAmbientMixer } from './useAmbientMixer'

/** One ambient mixer per study room page, shared by the Music settings tab and the timer's mini player. */
export const AmbientContext = createContext<ReturnType<typeof useAmbientMixer> | null>(null)

export function useAmbient() {
  const mixer = useContext(AmbientContext)
  if (!mixer) throw new Error('useAmbient must be used inside MusicProvider')
  return mixer
}
