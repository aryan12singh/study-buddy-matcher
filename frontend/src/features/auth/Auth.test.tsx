import { afterEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { AxiosError, AxiosHeaders } from 'axios'
import { AppRoutes } from '../../App'
import AuthProvider from '../../shared/auth/AuthProvider'
import { useAuth } from '../../shared/auth/useAuth'
import { api, getAccessToken, setAccessToken } from '../../shared/api/client'
import { deferred, response, studentAccount } from '../../test/renderApp'

const originalAdapter = api.defaults.adapter
afterEach(() => { vi.restoreAllMocks(); setAccessToken(null); api.defaults.adapter = originalAdapter })
function renderAuthenticatedApp(path: string) { return render(<MemoryRouter initialEntries={[path]}><AuthProvider><AppRoutes /></AuthProvider></MemoryRouter>) }

describe('auth and role boundaries', () => {
  it('redirects an unauthenticated protected route to login', async () => {
    setAccessToken(null)
    const get = vi.spyOn(api, 'get')
    renderAuthenticatedApp('/groups')
    expect(await screen.findByRole('heading', { name: 'Log in' })).toBeInTheDocument()
    expect(get).not.toHaveBeenCalled()
  })
  it('public registration sends no account role and protects private contact', async () => {
    setAccessToken(null)
    const post = vi.spyOn(api, 'post').mockResolvedValue(response({ token: 'new-token', expiresAt: '2030-01-01T00:00:00Z', account: studentAccount }))
    vi.spyOn(api, 'get').mockImplementation(async path => response(path === '/notifications/unread-count' ? { count: 0 } : []))
    renderAuthenticatedApp('/register')
    fireEvent.change(screen.getByLabelText('Email address'), { target: { value: 'NEW@example.test' } })
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'a-long-passphrase' } })
    fireEvent.change(screen.getByLabelText('Full name'), { target: { value: 'New Student' } })
    fireEvent.change(screen.getByLabelText('School'), { target: { value: 'SCIS' } })
    fireEvent.change(screen.getByLabelText('Programme'), { target: { value: 'Information Systems' } })
    fireEvent.change(screen.getByLabelText('Contact number'), { target: { value: '+65 9999 1111' } })
    fireEvent.click(screen.getByRole('button', { name: 'Create student account' }))
    await waitFor(() => expect(post).toHaveBeenCalledWith('/auth/register', { email: 'new@example.test', password: 'a-long-passphrase', name: 'New Student', school: 'SCIS', programme: 'Information Systems', yearOfStudy: 1, contactNumber: '+65 9999 1111' }))
  })
  it('expires the current session after a real Axios 401', async () => {
    setAccessToken('expired-token')
    api.defaults.adapter = async config => {
      throw new AxiosError('Unauthorized', 'ERR_BAD_REQUEST', config, undefined, { config, status: 401, statusText: 'Unauthorized', headers: new AxiosHeaders(), data: { code: 'UNAUTHENTICATED', message: 'Session expired' } })
    }
    renderAuthenticatedApp('/connections')
    expect(await screen.findByRole('heading', { name: 'Log in' })).toBeInTheDocument()
    expect(getAccessToken()).toBeNull()
  })
  it('does not restore an account from a late current-account result after logout', async () => {
    setAccessToken('old-token')
    const pending = deferred<never>()
    vi.spyOn(api, 'get').mockImplementation(() => pending.promise)
    function SessionProbe() { const auth = useAuth(); return <><p>{auth.account?.email || 'No session'}</p><button onClick={auth.logout}>Log out now</button></> }
    render(<AuthProvider><SessionProbe /></AuthProvider>)
    fireEvent.click(screen.getByRole('button', { name: 'Log out now' }))
    pending.resolve(response(studentAccount))
    await waitFor(() => expect(screen.getByText('No session')).toBeInTheDocument())
    expect(screen.queryByText(studentAccount.email)).not.toBeInTheDocument()
  })
  it('attaches the bearer token only to protected calls', async () => {
    setAccessToken('current-token')
    const requests: { url?: string; authorization: unknown }[] = []
    api.defaults.adapter = async config => { requests.push({ url: config.url, authorization: config.headers.Authorization }); return { config, status: 200, statusText: 'OK', headers: new AxiosHeaders(), data: [] } }
    await api.get('/connections')
    await api.post('/auth/login', { email: 'student@example.test', password: 'test-password' })
    expect(requests[0].authorization).toBe('Bearer current-token')
    expect(requests[1].authorization).toBeUndefined()
  })
  it('does not expire a new session when a superseded request returns 401', async () => {
    setAccessToken('old-token')
    const pending = deferred<void>()
    const adapter = vi.fn(async config => {
      await pending.promise
      throw new AxiosError('Unauthorized', 'ERR_BAD_REQUEST', config, undefined, { config, status: 401, statusText: 'Unauthorized', headers: new AxiosHeaders(), data: { message: 'Old session expired' } })
    })
    api.defaults.adapter = adapter
    const oldRequest = api.get('/connections').catch(error => error)
    await waitFor(() => expect(adapter).toHaveBeenCalledTimes(1))
    setAccessToken('new-token')
    pending.resolve()
    await oldRequest
    expect(getAccessToken()).toBe('new-token')
  })
})
