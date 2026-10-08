import { useEffect, useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import Button from './Button'

/** Generic outcome text only; no account fields or private form values in history. */
export default function RouteNotice() {
  const location = useLocation()
  const navigate = useNavigate()
  const seen = useRef(new Set<string>())
  const [notice, setNotice] = useState<{ key: string; message: string }>()
  useEffect(() => {
    let disposed = false
    queueMicrotask(() => {
      if (disposed) return
      const message = location.state?.notice
      if (typeof message === 'string' && message) {
        if (seen.current.has(location.key)) return
        seen.current.add(location.key)
        setNotice({ key: location.key, message })
        const state = { ...location.state, noticeConsumed: location.key }
        delete state.notice
        void navigate({ pathname: location.pathname, search: location.search, hash: location.hash }, { replace: true, state })
      } else {
        setNotice(previous => previous && location.state?.noticeConsumed === previous.key
          ? { ...previous, key: location.key } : undefined)
      }
    })
    return () => { disposed = true }
  }, [location.key, location.state, location.pathname, location.search, location.hash, navigate])
  if (!notice || notice.key !== location.key && location.state?.noticeConsumed !== notice.key) return null
  return <div className="route-notice success-panel">
    <p role="status"><span aria-hidden="true">✓ </span>{notice.message}</p>
    <Button aria-label="Dismiss success message" onClick={() => setNotice(undefined)}>×</Button>
  </div>
}
