import { useCallback, useLayoutEffect, useRef } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import type { NavigateOptions, To } from 'react-router-dom'

export function useViewLifetime() {
  const { key } = useLocation()
  const currentView = useRef<string | null>(null)
  useLayoutEffect(() => {
    currentView.current = key
    return () => { currentView.current = null }
  }, [key])
  return useCallback(() => currentView.current === key, [key])
}

/** A late action may refresh data, but must not navigate away from a newer view. */
export function useViewNavigation() {
  const navigate = useNavigate(), isCurrent = useViewLifetime()
  return useCallback((to: To, options?: NavigateOptions) => {
    if (isCurrent()) void navigate(to, options)
  }, [isCurrent, navigate])
}
