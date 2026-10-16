import { useCallback, useEffect, useRef, useState } from 'react'
import axios from 'axios'
import { errorMessage } from '../../shared/api/client'
import { getRoom, heartbeatRoom } from './api'
import type { PresenceState, RoomSnapshot } from './api'

/** A dropped connection, timeout or server fault says nothing about room access. */
function isTransient(error: unknown) {
  return axios.isAxiosError(error) && (!error.response || error.response.status >= 500)
}

export function useStudyRoom(groupId: number, clientId: string, valid: boolean) {
  const [state, setState] = useState<{ snapshot?: RoomSnapshot; error?: string; loading: boolean }>({ loading: valid })
  const [joinedHere, setJoinedHere] = useState(false)
  const [pollInterval, setPollInterval] = useState<number>()
  const [reconnecting, setReconnecting] = useState(false)
  const joined = useRef(false)
  const presence = useRef<PresenceState>('PRESENT')
  const request = useRef<AbortController | null>(null)
  const sequence = useRef(0)
  const cancelPending = useCallback(() => {
    request.current?.abort(); request.current = null; sequence.current++
  }, [])

  const accept = useCallback((snapshot: RoomSnapshot) => {
    cancelPending()
    setState({ snapshot, loading: false })
    setReconnecting(false)
    setPollInterval(snapshot.room.pollIntervalMillis)
  }, [cancelPending])
  const markJoined = useCallback((value: boolean) => {
    cancelPending()
    joined.current = value
    setJoinedHere(value)
  }, [cancelPending])
  const refresh = useCallback(async () => {
    if (!valid || request.current) return
    const controller = new AbortController()
    request.current = controller
    const generation = ++sequence.current
    try {
      const snapshot = joined.current
        ? await heartbeatRoom(groupId, clientId, presence.current, controller.signal)
        : await getRoom(groupId, controller.signal)
      if (!controller.signal.aborted && generation === sequence.current) {
        setState({ snapshot, loading: false }); setReconnecting(false); setPollInterval(snapshot.room.pollIntervalMillis)
      }
    } catch (error) {
      if (!controller.signal.aborted && generation === sequence.current && !axios.isCancel(error)) {
        if (isTransient(error)) {
          // Keep the room until the server itself rejects the lease; the next poll retries.
          setState(previous => previous.snapshot ? previous : { loading: false, error: errorMessage(error) })
          setReconnecting(true)
        } else {
          setReconnecting(false)
          markJoined(false)
          setState({ loading: false, error: errorMessage(error) })
        }
      }
    } finally { if (request.current === controller) request.current = null }
  }, [groupId, clientId, valid, markJoined])

  useEffect(() => {
    let disposed = false
    queueMicrotask(() => { if (!disposed) void refresh() })
    const onRefresh = () => { void refresh() }
    window.addEventListener('focus', onRefresh)
    window.addEventListener('resources-changed', onRefresh)
    const onVisible = () => { if (!document.hidden) onRefresh() }
    document.addEventListener('visibilitychange', onVisible)
    return () => {
      disposed = true; cancelPending()
      window.removeEventListener('focus', onRefresh)
      window.removeEventListener('resources-changed', onRefresh)
      document.removeEventListener('visibilitychange', onVisible)
    }
  }, [refresh, cancelPending])
  useEffect(() => {
    if (!pollInterval) return
    const interval = window.setInterval(() => { void refresh() }, pollInterval)
    return () => window.clearInterval(interval)
  }, [pollInterval, refresh])

  return { ...state, reconnecting, joinedHere, markJoined, accept, refresh, setPresence: (value: PresenceState) => { presence.current = value } }
}
