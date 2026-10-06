import { useCallback, useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { api, errorMessage, getAccessToken, refreshResources, setAccessToken } from '../api/client'
import { AuthContext } from './context'
import type { Account } from './context'

type AuthResponse = {
  token: string
  expiresAt: string
  account: Account
}

export default function AuthProvider({ children }: { children: ReactNode }) {
  const [account, setAccount] = useState<Account | null>(null)
  const [loading, setLoading] = useState(Boolean(getAccessToken()))
  const [error, setError] = useState<string>()
  const [retryCount, setRetryCount] = useState(0)
  const logout = useCallback(
    () => {
      setAccessToken(null)
      setAccount(null)
      setError(undefined)
      setLoading(false)
      refreshResources(true)
    },
    []
  )
  useEffect(
    () => {
      const abort = new AbortController()
      const requestToken = getAccessToken()
      if (requestToken) {
        api.get<Account>('/auth/me', { signal: abort.signal }).then(response => {
          if (!abort.signal.aborted && getAccessToken() === requestToken) setAccount(response.data)
        }).catch(failure => {
          if (!abort.signal.aborted && getAccessToken() === requestToken) setError(errorMessage(failure))
        }).finally(() => {
          if (!abort.signal.aborted && getAccessToken() === requestToken) setLoading(false)
        })
      }
      return () => abort.abort()
    },
    [retryCount]
  )
  useEffect(
    () => {
      window.addEventListener('session-expired', logout)
      return () => window.removeEventListener('session-expired', logout)
    },
    [logout]
  )

  async function authenticate(path: string, input: unknown) {
    const response = await api.post<AuthResponse>(path, input)
    refreshResources(true)
    setAccessToken(response.data.token)
    setAccount(response.data.account)
    setError(undefined)
    setLoading(false)
    return response.data.account
  }
  return (
    <AuthContext.Provider value={{
      account,
      loading,
      error,
      logout,
      retry: () => {
        setLoading(true)
        setError(undefined)
        setRetryCount(value => value + 1)
      },
      login: (email, password) => authenticate('/auth/login', { email: email.trim().toLowerCase(), password }),
      register: input => authenticate('/auth/register', { ...input, email: input.email.trim().toLowerCase() })
    }}>{children}</AuthContext.Provider>
  )
}
