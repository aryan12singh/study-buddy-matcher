import { createContext, useContext } from 'react'

/** Whether the music tab has been opened on this page, which the corner prompt needs to know. */
export type MusicSettingsController = {
  /** True once the music tab has been shown; the player then stays loaded. */
  opened: boolean
  markOpened: () => void
}

export const MusicSettingsContext = createContext<MusicSettingsController | null>(null)

export function useMusicSettings() {
  const controller = useContext(MusicSettingsContext)
  if (!controller) throw new Error('useMusicSettings must be used inside MusicProvider')
  return controller
}
