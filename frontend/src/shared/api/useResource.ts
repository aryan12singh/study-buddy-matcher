import { useCallback, useEffect, useRef, useState } from 'react'
import axios from 'axios'
import { errorMessage } from './client'

const configuredRefresh = Number(import.meta.env.VITE_REFRESH_INTERVAL_MS)
export const REFRESH_INTERVAL_MS = Number.isFinite(configuredRefresh) && configuredRefresh >= 15000
  ? Math.min(configuredRefresh, 300000) : 30000

export function useResource<T>(key: string, loader: (signal: AbortSignal) => Promise<T>, enabled = true, backgroundRefresh = true) {
  const loaderRef = useRef(loader)
  useEffect(() => { loaderRef.current = loader })
  const sequence = useRef(0)
  const controller = useRef<AbortController | null>(null)
  const [state, setState] = useState<{ key: string; data?: T; loading: boolean; error?: string }>({ key, loading: enabled })

  const reload = useCallback((purge = false) => {
    if (!enabled) return
    controller.current?.abort()
    controller.current = new AbortController()
    const signal = controller.current.signal
    const request = ++sequence.current
    setState(previous => ({ key, data: purge || previous.key !== key ? undefined : previous.data, loading: true }))
    loaderRef.current(signal).then(data => {
      if (sequence.current === request && !signal.aborted) setState({ key, data, loading: false })
    }).catch(error => {
      if (sequence.current === request && !signal.aborted && !axios.isCancel(error)) {
        // Never leave an earlier private response on screen after a failed refresh.
        setState({ key, loading: false, error: errorMessage(error) })
      }
    })
  }, [key, enabled])
  const cancel = useCallback(() => { controller.current?.abort(); sequence.current++ }, [])

  useEffect(() => {
    if (!enabled) return
    let disposed = false
    queueMicrotask(() => { if (!disposed) reload(true) })
    const onChange = (event: Event) => reload((event as CustomEvent<{ purge: boolean }>).detail?.purge)
    // Background revalidation keeps the last response visible until it is replaced, so pages do
    // not blank on every focus or poll. A failed refresh still clears it, and permission changes
    // (disconnect, logout, 401) purge first through refreshResources(true).
    const onFocus = () => { if (backgroundRefresh) reload() }
    const onVisible = () => { if (backgroundRefresh && !document.hidden) reload() }
    window.addEventListener('resources-changed', onChange)
    window.addEventListener('focus', onFocus)
    document.addEventListener('visibilitychange', onVisible)
    const timer = backgroundRefresh ? window.setInterval(() => { if (!document.hidden) reload() }, REFRESH_INTERVAL_MS) : undefined
    return () => {
      disposed = true
      cancel()
      window.clearInterval(timer)
      window.removeEventListener('resources-changed', onChange)
      window.removeEventListener('focus', onFocus)
      document.removeEventListener('visibilitychange', onVisible)
    }
  }, [key, enabled, reload, backgroundRefresh, cancel])

  return { data: enabled && state.key === key ? state.data : undefined, loading: enabled && (state.key === key ? state.loading : true),
    error: enabled && state.key === key ? state.error : undefined, reload }
}
