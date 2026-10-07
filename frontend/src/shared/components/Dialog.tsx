import { useEffect, useId, useRef } from 'react'
import type { ReactNode } from 'react'
import { createPortal } from 'react-dom'
import Button from './Button'

const openPanels = new Set<HTMLElement>()
let originalOverflow: string | undefined

export default function Dialog({ title, onClose, busy = false, children }: {
  title: string
  onClose: () => void
  busy?: boolean
  children: ReactNode
}) {
  const titleId = useId()
  const panel = useRef<HTMLDivElement>(null)
  useEffect(
    () => {
      const previous = document.activeElement as HTMLElement | null
      const currentPanel = panel.current
      if (!currentPanel) return
      if (!openPanels.size) originalOverflow = document.body.style.overflow
      openPanels.add(currentPanel)
      document.body.style.overflow = 'hidden'
      const first = panel.current?.querySelector<HTMLElement>('button:not(:disabled), input, textarea, select, a[href]')
        ;
      (first || panel.current)?.focus()
      return () => {
        openPanels.delete(currentPanel)
        if (!openPanels.size) {
          document.body.style.overflow = originalOverflow || ''
          originalOverflow = undefined
        }
        if (previous?.isConnected) previous.focus()
      }
    },
    []
  )

  function handleKeyDown(event: React.KeyboardEvent) {
    if (event.key === 'Escape' && !busy) {
      event.preventDefault()
      onClose()
    }
    if (event.key !== 'Tab') return
    const controls = Array.from(panel.current?.querySelectorAll<HTMLElement>('button:not(:disabled), input:not(:disabled), textarea:not(:disabled), select:not(:disabled), a[href], [tabindex="0"]') || [])
    if (!controls.length) {
      event.preventDefault()
      panel.current?.focus()
      return
    }
    const first = controls[0],
      last = controls[controls.length - 1]
    if (event.shiftKey && (document.activeElement === first || document.activeElement === panel.current)) {
      event.preventDefault()
      last.focus()
    }
    else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault()
      first.focus()
    }
  }
  return createPortal(
    <div className="dialog-backdrop">
      <div
        ref={panel}
        className="retro-window dialog-window"
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        tabIndex={-1}
        onKeyDown={handleKeyDown}
      >
        <div className="window-titlebar">
          <h2 id={titleId}>{title}</h2>
          <Button
            disabled={busy}
            aria-label={`Close ${title}`}
            onClick={onClose}
          >
            ×
          </Button>
        </div>
        <div className="window-content">{children}</div>
      </div>
    </div>,
    document.body
  )
}
