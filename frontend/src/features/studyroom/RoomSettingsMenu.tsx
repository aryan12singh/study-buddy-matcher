import { useEffect, useId, useRef, useState } from 'react'
import type { KeyboardEvent, ReactNode } from 'react'
import { onOpenRoomSettings } from './roomSettingsEvents'
import './windowControls.css'

export type RoomSettingsTab = {
  id: string; label: string; icon: string; content: ReactNode
  /** Kept mounted while hidden, for panels that must keep running (audio, live weather). */
  persistent?: boolean
  /** Mounted the first time it is shown, then kept mounted, for panels that are slow to load but must keep playing. */
  keepAfterOpen?: boolean
}

/**
 * A tabbed settings menu. Persistent panels stay mounted when hidden; keep-after-open panels stay
 * mounted once shown; others mount only while shown.
 */
export default function RoomSettingsMenu({ tabs }: { tabs: RoomSettingsTab[] }) {
  const [open, setOpen] = useState(false)
  const [active, setActive] = useState(tabs[0].id)
  const id = useId()
  const menu = useRef<HTMLDivElement>(null)
  const toggle = useRef<HTMLButtonElement>(null)
  const tabButtons = useRef<Record<string, HTMLButtonElement | null>>({})
  const [visited, setVisited] = useState<ReadonlySet<string>>(new Set())
  const shown = open ? active : undefined
  if (shown && !visited.has(shown)) setVisited(new Set(visited).add(shown))

  // Something else on the page may ask for a tab, such as the music prompt in the timer's corner.
  useEffect(() => onOpenRoomSettings(tab => {
    if (!tabs.some(candidate => candidate.id === tab)) return
    setActive(tab)
    setOpen(true)
  }), [tabs])

  useEffect(() => {
    if (!open) return
    tabButtons.current[active]?.focus()
    const closeOutside = (event: PointerEvent) => {
      // A confirmation dialog opened from a tab is portalled outside the menu but still belongs to it.
      if (event.target instanceof Element && event.target.closest('[role="dialog"]')) return
      if (event.target instanceof Node && !menu.current?.contains(event.target)) setOpen(false)
    }
    document.addEventListener('pointerdown', closeOutside)
    return () => document.removeEventListener('pointerdown', closeOutside)
    // Focus moves to the active tab only when the menu opens.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open])

  function close() {
    setOpen(false)
    toggle.current?.focus()
  }
  function select(tab: string) {
    setActive(tab)
    tabButtons.current[tab]?.focus()
  }
  function moveBetweenTabs(event: KeyboardEvent) {
    const index = tabs.findIndex(tab => tab.id === active)
    const next = event.key === 'ArrowRight' ? index + 1 : event.key === 'ArrowLeft' ? index - 1
      : event.key === 'Home' ? 0 : event.key === 'End' ? tabs.length - 1 : null
    if (next === null) return
    event.preventDefault()
    select(tabs[(next + tabs.length) % tabs.length].id)
  }

  return <div ref={menu} className="room-settings-menu"
    onKeyDown={event => { if (event.key === 'Escape' && open) { event.preventDefault(); close() } }}>
    <button ref={toggle} type="button" className="room-window-control room-settings-toggle" aria-label="Settings" title="Settings"
      aria-expanded={open} aria-controls={`${id}-menu`} onClick={() => setOpen(value => !value)}>
      <span className="window-control-icon settings" aria-hidden="true">⚙</span>
    </button>
    <div id={`${id}-menu`} className="room-settings-popover" role="region" aria-label="Room settings" hidden={!open}>
      <div className="room-settings-tabs">
        <div role="tablist" aria-label="Room settings">
          {tabs.map(tab => <button key={tab.id} ref={element => { tabButtons.current[tab.id] = element }} type="button"
            role="tab" id={`${id}-${tab.id}-tab`} aria-selected={active === tab.id} aria-controls={`${id}-${tab.id}-panel`}
            tabIndex={active === tab.id ? 0 : -1} className="room-settings-tab"
            onClick={() => setActive(tab.id)} onKeyDown={moveBetweenTabs}>
            <span className="room-settings-tab-icon" aria-hidden="true">{tab.icon}</span>{tab.label}
          </button>)}
        </div>
        <button type="button" className="room-settings-close" aria-label="Close settings" onClick={close}>×</button>
      </div>
      {tabs.map(tab => <div key={tab.id} role="tabpanel" id={`${id}-${tab.id}-panel`} aria-labelledby={`${id}-${tab.id}-tab`}
        className="room-settings-panel" hidden={active !== tab.id}>
        {(tab.persistent || (tab.keepAfterOpen && visited.has(tab.id)) || (open && active === tab.id)) && tab.content}
      </div>)}
    </div>
  </div>
}
