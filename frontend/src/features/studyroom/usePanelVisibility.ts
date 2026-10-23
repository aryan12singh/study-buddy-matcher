import { useCallback, useRef, useState } from 'react'

/** Hides a room's panel so only its timer shows over the background, returning focus when it comes back. */
export function usePanelVisibility() {
  const [hidden, setHidden] = useState(false)
  const hideButton = useRef<HTMLButtonElement>(null)
  const hide = useCallback(() => setHidden(true), [])
  const show = useCallback(() => {
    setHidden(false)
    requestAnimationFrame(() => hideButton.current?.focus())
  }, [])
  return { hidden, hide, show, hideButton }
}

export type PanelVisibility = ReturnType<typeof usePanelVisibility>
