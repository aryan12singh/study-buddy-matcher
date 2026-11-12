const OPEN_EVENT = 'open-room-settings'

let listening = 0
/** A request made before any menu was listening, kept for the first menu that starts listening. */
let pendingTab: string | undefined

/**
 * Opens the room's settings menu on one tab from elsewhere on the page, such as the music prompt
 * in the timer's corner. A page has one settings menu, which listens for this. A request made
 * while no menu is on the page yet, such as returning from Spotify sign-in while the room page is
 * still loading, waits for the next menu to appear.
 */
export function openRoomSettings(tab: string) {
  if (listening === 0) {
    pendingTab = tab
    return
  }
  window.dispatchEvent(new CustomEvent(OPEN_EVENT, { detail: { tab } }))
}

export function onOpenRoomSettings(listener: (tab: string) => void) {
  const handle = (event: Event) => listener((event as CustomEvent<{ tab: string }>).detail.tab)
  window.addEventListener(OPEN_EVENT, handle)
  listening++
  if (pendingTab !== undefined) {
    const tab = pendingTab
    pendingTab = undefined
    listener(tab)
  }
  return () => {
    window.removeEventListener(OPEN_EVENT, handle)
    listening--
  }
}
