import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { errorMessage, fieldErrors, refreshResources } from './client'

export type ActionOptions = {
  /** Refetch the shared resources after a success (the default). Off for actions no other view shows, such as a chat reply. */
  refresh?: boolean
  /** The screen's own wording for a failure it can explain better than the server; undefined keeps the server's message. */
  describeError?: (failure: unknown) => string | undefined
}

export type ActionResult<T> = { ok: true; value: T } | { ok: false; failure: unknown }

/** Pending, error and success state for one kind of action on a screen. The returned object only changes when that state does. */
export function useAction(options: ActionOptions = {}) {
  const [pending, setPending] = useState(false)
  const [error, setError] = useState<string>()
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [success, setSuccess] = useState<string>()
  const optionsRef = useRef(options)
  useEffect(() => { optionsRef.current = options })

  const run = useCallback(async <T,>(action: () => Promise<T>, message?: string, purge = false): Promise<ActionResult<T>> => {
    setPending(true); setError(undefined); setSuccess(undefined); setErrors({})
    try {
      const value = await action()
      if (optionsRef.current.refresh !== false) refreshResources(purge)
      setSuccess(message)
      return { ok: true, value }
    } catch (failure) {
      setError(optionsRef.current.describeError?.(failure) ?? errorMessage(failure)); setErrors(fieldErrors(failure))
      return { ok: false, failure }
    } finally { setPending(false) }
  }, [])
  const clear = useCallback(() => { setError(undefined); setSuccess(undefined); setErrors({}) }, [])
  /** Shows a success message for an action completed elsewhere, such as in a dialog that has closed. */
  const announce = useCallback((message: string) => { setError(undefined); setErrors({}); setSuccess(message) }, [])
  return useMemo(() => ({ pending, error, errors, success, run, clear, announce }), [pending, error, errors, success, run, clear, announce])
}
