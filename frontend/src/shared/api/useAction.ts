import { useState } from 'react'
import { errorMessage, fieldErrors, refreshResources } from './client'

export function useAction() {
  const [pending, setPending] = useState(false)
  const [error, setError] = useState<string>()
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [success, setSuccess] = useState<string>()

  async function run<T>(action: () => Promise<T>, message?: string, purge = false): Promise<{ ok: true; value: T } | { ok: false }> {
    setPending(true); setError(undefined); setSuccess(undefined); setErrors({})
    try {
      const value = await action()
      refreshResources(purge)
      setSuccess(message)
      return { ok: true, value }
    } catch (failure) {
      setError(errorMessage(failure)); setErrors(fieldErrors(failure))
      return { ok: false }
    } finally { setPending(false) }
  }
  function clear() { setError(undefined); setSuccess(undefined); setErrors({}) }
  /** Shows a success message for an action completed elsewhere, such as in a dialog that has closed. */
  function announce(message: string) { setError(undefined); setErrors({}); setSuccess(message) }
  return { pending, error, errors, success, run, clear, announce }
}
