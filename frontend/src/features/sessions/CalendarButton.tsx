import { useEffect, useRef } from 'react'
import axios from 'axios'
import { getCalendar } from './api'
import { useAction } from '../../shared/api/useAction'
import Button from '../../shared/components/Button'

/** Each download belongs to this mounted account/session view; late responses cannot create files. */
export default function CalendarButton({ sessionId }: { sessionId: number }) {
  const request = useRef<AbortController | null>(null)
  // A download changes nothing other views show.
  const action = useAction({ refresh: false })
  useEffect(() => () => { request.current?.abort() }, [sessionId])
  async function download() {
    if (request.current) return
    const controller = new AbortController(); request.current = controller
    await action.run(async () => {
      try {
        const content = await getCalendar(sessionId, controller.signal)
        if (!controller.signal.aborted && request.current === controller) saveFile(content, `study-session-${sessionId}.ics`)
      } catch (failure) {
        // A download abandoned by leaving the page is not an error worth showing.
        if (!controller.signal.aborted && !axios.isCancel(failure)) throw failure
      }
    })
    if (request.current === controller) request.current = null
  }
  return <div>
    <Button disabled={action.pending} onClick={() => void download()}>{action.pending ? 'Preparing calendar…' : 'Download calendar (.ics)'}</Button>
    {action.error && <p className="field-error" role="alert">{action.error}</p>}
  </div>
}

function saveFile(content: Blob, name: string) {
  const url = URL.createObjectURL(content)
  const link = document.createElement('a')
  try {
    link.href = url; link.download = name
    document.body.appendChild(link); link.click()
  } finally {
    link.remove()
    // Give the browser an event-loop turn to consume the URL, then always release it.
    window.setTimeout(() => URL.revokeObjectURL(url), 0)
  }
}
