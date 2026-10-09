import { afterEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { AxiosError, AxiosHeaders } from 'axios'
import { AppRoutes } from '../../App'
import AuthProvider from '../../shared/auth/AuthProvider'
import { AuthContext } from '../../shared/auth/context'
import { useAuth } from '../../shared/auth/useAuth'
import { api, getAccessToken, SESSION_TOKEN_KEY, setAccessToken } from '../../shared/api/client'
import { adminAccount, deferred, response, studentAccount } from '../../test/renderApp'

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
  it('logs in with normalized identity and stores only the session token', async () => {
    setAccessToken(null)
    const post = vi.spyOn(api, 'post').mockResolvedValue(response({ token: 'session-test-token', expiresAt: '2030-01-01T00:00:00Z', account: studentAccount }))
    // Single-object endpoints stay unanswered here; this test is about login, not the home page's data.
    vi.spyOn(api, 'get').mockImplementation(async path => path === '/profile/me' || path === '/students/me/summary'
      ? new Promise(() => {}) : response(path === '/notifications/unread-count' ? { count: 0 } : []))
    renderAuthenticatedApp('/login')
    fireEvent.change(screen.getByLabelText('Email address'), { target: { value: '  STUDENT@example.test  ' } })
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'test-password' } })
    fireEvent.click(screen.getByRole('button', { name: 'Log in' }))
    expect(await screen.findByRole('heading', { name: 'Welcome back, Priya' })).toBeInTheDocument()
    expect(post).toHaveBeenCalledWith('/auth/login', { email: 'student@example.test', password: 'test-password' })
    expect(sessionStorage.getItem(SESSION_TOKEN_KEY)).toBe('session-test-token')
    expect(sessionStorage.getItem('contactNumber')).toBeNull()
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
  it('guards admin routes from a student without requesting admin data', async () => {
    const get = vi.spyOn(api, 'get').mockImplementation(async path => response(path === '/notifications/unread-count' ? { count: 0 } : []))
    render(<AuthContext.Provider value={{ account: studentAccount, loading: false, retry: vi.fn(), logout: vi.fn(), login: vi.fn(), register: vi.fn() }}><MemoryRouter initialEntries={['/admin/users']}><AppRoutes /></MemoryRouter></AuthContext.Provider>)
    expect(await screen.findByRole('heading', { name: 'Connections' })).toBeInTheDocument()
    expect(get.mock.calls.some(([path]) => path.startsWith('/admin'))).toBe(false)
  })
  it('keeps administrator sessions outside the student group routes', async () => {
    const get = vi.spyOn(api, 'get').mockImplementation(async path => response(path.endsWith('/summary') ? { total: 1, active: 1, inactive: 0, students: 0, admins: 1 } : []))
    render(<AuthContext.Provider value={{ account: adminAccount, loading: false, retry: vi.fn(), logout: vi.fn(), login: vi.fn(), register: vi.fn() }}><MemoryRouter initialEntries={['/groups/new']}><AppRoutes /></MemoryRouter></AuthContext.Provider>)
    expect(await screen.findByRole('heading', { name: 'User accounts' })).toBeInTheDocument()
    expect(get.mock.calls.some(([path]) => path.startsWith('/groups') || path.startsWith('/notifications'))).toBe(false)
  })
  it('removes all private rendered state and session storage on logout', async () => {
    setAccessToken('saved-token')
    vi.spyOn(api, 'get').mockImplementation(async path => response(path === '/auth/me' ? studentAccount : path === '/notifications/unread-count' ? { count: 1 } : { id: 2, name: 'Jamie', school: 'SCIS', programme: 'IS', yearOfStudy: 2, coursesTaken: [], targetCourse: null, preferredStudyMode: null, studyGoals: [], preferredGroupSizeMin: null, preferredGroupSizeMax: null, availability: [], relationship: { state: 'CONNECTED', requestId: null, connectionId: 7 }, contactNumber: '+65 9999 1111' }))
    renderAuthenticatedApp('/students/2')
    await screen.findByText('+65 9999 1111')
    fireEvent.click(screen.getByRole('button', { name: 'Log out' }))
    expect(await screen.findByRole('heading', { name: 'Log in' })).toBeInTheDocument()
    expect(screen.queryByText('+65 9999 1111')).not.toBeInTheDocument()
    expect(getAccessToken()).toBeNull()
    expect(sessionStorage.getItem(SESSION_TOKEN_KEY)).toBeNull()
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
