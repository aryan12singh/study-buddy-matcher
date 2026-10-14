import { useEffect, useState } from 'react'
import type { StudySession } from './api'

const CLOCK_REFRESH_MS = 60_000

/** One clock for the entire view, waking at the next start/end boundary. */
export function useSessionClock(sessions: readonly StudySession[]) {
  const [now, setNow] = useState(() => Date.now())
  useEffect(() => {
    let timer: number
    function update() {
      window.clearTimeout(timer)
      const time = Date.now()
      setNow(time)
      let delay = CLOCK_REFRESH_MS
      for (const session of sessions) {
        if (session.status !== 'SCHEDULED') continue
        for (const occurrence of session.occurrences) {
          for (const boundary of [Date.parse(occurrence.startAt), Date.parse(occurrence.endAt)]) {
            if (boundary > time) delay = Math.min(delay, boundary - time)
          }
        }
      }
      timer = window.setTimeout(update, Math.max(1, delay))
    }
    function onVisible() { if (!document.hidden) update() }
    update()
    window.addEventListener('focus', update)
    document.addEventListener('visibilitychange', onVisible)
    return () => {
      window.clearTimeout(timer)
      window.removeEventListener('focus', update)
      document.removeEventListener('visibilitychange', onVisible)
    }
  }, [sessions])
  return now
}
