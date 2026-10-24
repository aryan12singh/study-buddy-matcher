const OPEN_EVENT = 'open-room-settings'

/**
 * Opens the room's settings menu on one tab from elsewhere on the page, such as the music prompt
 * in the timer's corner. A page has one settings menu, which listens for this.
 */
export function openRoomSettings(tab: string) {
  window.dispatchEvent(new CustomEvent(OPEN_EVENT, { detail: { tab } }))
}

export function onOpenRoomSettings(listener: (tab: string) => void) {
  const handle = (event: Event) => listener((event as CustomEvent<{ tab: string }>).detail.tab)
  window.addEventListener(OPEN_EVENT, handle)
  return () => window.removeEventListener(OPEN_EVENT, handle)
}
