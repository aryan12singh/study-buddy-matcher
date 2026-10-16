import { useEffect, useRef, useState } from 'react'
import axios from 'axios'
import { getCalendar } from './api'
import { errorMessage } from '../../shared/api/client'
import Button from '../../shared/components/Button'

/** Each download belongs to this mounted account/session view; late responses cannot create files. */
export default function CalendarButton({ sessionId }: { sessionId: number }) {
  const request = useRef<AbortController | null>(null)
  const [pending, setPending] = useState(false)
  const [error, setError] = useState<string>()
  useEffect(() => () => { request.current?.abort() }, [sessionId])
  async function download() {
    if (request.current) return
    const controller = new AbortController(); request.current = controller
    setPending(true); setError(undefined)
    try {
      const content = await getCalendar(sessionId, controller.signal)
      if (controller.signal.aborted || request.current !== controller) return
      const url = URL.createObjectURL(content)
      const link = document.createElement('a')
      try {
        link.href = url; link.download = `study-session-${sessionId}.ics`
        document.body.appendChild(link); link.click()
      } finally {
        link.remove()
        // Give the browser an event-loop turn to consume the URL, then always release it.
        window.setTimeout(() => URL.revokeObjectURL(url), 0)
      }
    } catch (failure) {
      if (!controller.signal.aborted && !axios.isCancel(failure)) setError(errorMessage(failure))
    } finally {
      if (!controller.signal.aborted && request.current === controller) { request.current = null; setPending(false) }
    }
  }
  return <div>
    <Button disabled={pending} onClick={() => void download()}>{pending ? 'Preparing calendar…' : 'Download calendar (.ics)'}</Button>
    {error && <p className="field-error" role="alert">{error}</p>}
  </div>
}
