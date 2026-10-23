import { useCallback, useEffect, useState } from 'react'

/**
 * Expands the room over the whole page and asks the browser for full screen. The page itself goes
 * full screen, not the room, so confirmation dialogs portalled to the body stay visible. Browsers
 * without the Fullscreen API still get the expanded room.
 */
export function useRoomFullscreen() {
  const [expanded, setExpanded] = useState(false)

  const exit = useCallback(() => {
    setExpanded(false)
    if (document.fullscreenElement) void document.exitFullscreen().catch(() => {})
  }, [])

  const enter = useCallback(() => {
    setExpanded(true)
    void document.documentElement.requestFullscreen?.().catch(() => {})
  }, [])

  useEffect(() => {
    if (!expanded) return
    // Leaving browser full screen (Esc, F11) also collapses the room.
    const onChange = () => { if (!document.fullscreenElement) setExpanded(false) }
    // Escape collapses the in-page fallback unless something inside, like the settings menu, handled it.
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && !event.defaultPrevented && !document.fullscreenElement) setExpanded(false)
    }
    const overflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    document.addEventListener('fullscreenchange', onChange)
    document.addEventListener('keydown', onKeyDown)
    return () => {
      document.body.style.overflow = overflow
      document.removeEventListener('fullscreenchange', onChange)
      document.removeEventListener('keydown', onKeyDown)
    }
  }, [expanded])

  // Leaving the room page must not strand the browser in full screen.
  useEffect(() => () => { if (document.fullscreenElement) void document.exitFullscreen().catch(() => {}) }, [])

  return { expanded, toggle: expanded ? exit : enter }
}
