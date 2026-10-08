import { useState } from 'react'
import Dialog from './Dialog'
import Button from './Button'

/** Closing a dialog is a local action; route exits are handled by useFormExit. */
export function useDraftClose(dirty: boolean, onClose: () => void) {
  const [closing, setClosing] = useState(false)
  return {
    requestClose: () => { if (dirty) setClosing(true); else onClose() },
    confirmation: closing ? <Dialog title="Discard this draft?" onClose={() => setClosing(false)}>
      <p>Your draft has not been sent. Keep editing, or discard it and close this window.</p>
      <div className="actions dialog-actions">
        <Button onClick={() => setClosing(false)}>Keep editing</Button>
        <Button variant="danger" onClick={onClose}>Discard draft</Button>
      </div>
    </Dialog> : null
  }
}
