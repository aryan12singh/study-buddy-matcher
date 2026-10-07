import { useCallback, useLayoutEffect, useRef } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import type { NavigateOptions, To } from 'react-router-dom'

/** A late action may refresh data, but must not navigate away from a newer view. */
export function useViewNavigation() {
  const navigate = useNavigate(), { key } = useLocation()
  const currentView = useRef<string | null>(null)
  useLayoutEffect(() => {
    currentView.current = key
    return () => { currentView.current = null }
  }, [key])
  return useCallback((to: To, options?: NavigateOptions) => {
    if (currentView.current === key) void navigate(to, options)
  }, [key, navigate])
}
