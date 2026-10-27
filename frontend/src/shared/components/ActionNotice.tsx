import { useEffect, useState } from 'react'
import Button from './Button'

/** Success messages step aside on their own after this long; errors stay until dismissed. */
const SUCCESS_VISIBLE_MS = 5000

export default function ActionNotice({ error, success }: { error?: string; success?: string }) {
  // What is showing, reset whenever a new message arrives so a repeated action shows again.
  const [shown, setShown] = useState({ error, success })
  const [received, setReceived] = useState({ error, success })
  if (received.error !== error || received.success !== success) {
    setReceived({ error, success })
    setShown({ error, success })
  }

  useEffect(() => {
    if (!shown.success) return
    const timer = window.setTimeout(() => setShown(current => ({ ...current, success: undefined })), SUCCESS_VISIBLE_MS)
    return () => window.clearTimeout(timer)
  }, [shown.success])

  return (
    <>
      {shown.error && (
        <div className="action-notice error-panel">
          <p role="alert"><span aria-hidden="true">! </span>{shown.error}</p>
          <Button aria-label="Dismiss error message" onClick={() => setShown(current => ({ ...current, error: undefined }))}>×</Button>
        </div>
      )}
      {shown.success && (
        <div className="action-notice success-panel">
          <p role="status"><span aria-hidden="true">✓ </span>{shown.success}</p>
          <Button aria-label="Dismiss success message" onClick={() => setShown(current => ({ ...current, success: undefined }))}>×</Button>
        </div>
      )}
    </>
  )
}
