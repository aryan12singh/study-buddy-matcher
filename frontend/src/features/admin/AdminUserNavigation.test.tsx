import { afterEach, describe, expect, it, vi } from 'vitest'
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import { api } from '../../shared/api/client'
import { adminAccount, deferred, response } from '../../test/renderApp'
import { AuthContext } from '../../shared/auth/context'
import AdminUserFormPage from './AdminUserFormPage'
import AdminUserDetailPage from './AdminUserDetailPage'
import { within } from '@testing-library/react'

const detail = { account: { id: 100, email: 'admin@example.test', role: 'ADMIN', name: null,
  active: true, createdAt: '2026-10-01T00:00:00Z', lastLoginAt: null }, profile: null, usage: null }
function CurrentPath() {
  const location = useLocation()
  return <><p>Current path: {location.pathname}</p><p>Current entry: {location.key}</p></>
}
function BrowserBack() {
  const navigate = useNavigate()
  return <button onClick={() => navigate(-1)}>Browser back</button>
}
afterEach(() => vi.restoreAllMocks())

describe('administrator save navigation', () => {
  it('does not redirect after browser back leaves a pending permanent deletion', async () => {
    vi.spyOn(api, 'get').mockResolvedValue(response(detail))
    const save = deferred<never>()
    const remove = vi.spyOn(api, 'delete').mockImplementation(() => save.promise)
    const auth = { account: adminAccount, loading: false, login: vi.fn(), register: vi.fn(), logout: vi.fn(), retry: vi.fn() }
    render(<AuthContext.Provider value={auth}><MemoryRouter initialEntries={['/groups', '/admin/users/100']}>
      <CurrentPath /><BrowserBack />
      <Routes>
        <Route path="/admin/users/:id" element={<AdminUserDetailPage />} />
        <Route path="*" element={<p>Destination page</p>} />
      </Routes>
    </MemoryRouter></AuthContext.Provider>)
    fireEvent.click(await screen.findByRole('button', { name: 'Delete account permanently' }))
    const dialog = screen.getByRole('dialog')
    fireEvent.change(within(dialog).getByLabelText('Type admin@example.test to confirm'), { target: { value: detail.account.email } })
    fireEvent.click(within(dialog).getByRole('button', { name: 'Delete permanently' }))
    await waitFor(() => expect(remove).toHaveBeenCalledTimes(1))
    fireEvent.click(screen.getByRole('button', { name: 'Browser back' }))
    expect(screen.getByText('Current path: /groups')).toBeInTheDocument()
    await act(async () => { save.resolve(response(undefined)) })
    expect(screen.getByText('Current path: /groups')).toBeInTheDocument()
  })
  it.each(['create', 'edit'] as const)('does not redirect after leaving a pending %s form', async mode => {
    vi.spyOn(api, 'get').mockResolvedValue(response(detail))
    const save = deferred<never>()
    const write = vi.spyOn(api, mode === 'create' ? 'post' : 'put').mockImplementation(() => save.promise)
    render(<MemoryRouter initialEntries={[mode === 'create' ? '/admin/users/new' : '/admin/users/100/edit']}>
      <CurrentPath />
      <Routes>
        <Route path="/admin/users/new" element={<AdminUserFormPage />} />
        <Route path="/admin/users/:id/edit" element={<AdminUserFormPage />} />
        <Route path="*" element={<p>Destination page</p>} />
      </Routes>
    </MemoryRouter>)
    if (mode === 'create') {
      fireEvent.change(screen.getByLabelText('Account role'), { target: { value: 'ADMIN' } })
      fireEvent.change(screen.getByLabelText('Email address'), { target: { value: detail.account.email } })
      fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'a-long-passphrase' } })
    } else await screen.findByDisplayValue(detail.account.email)
    fireEvent.click(screen.getByRole('button', { name: mode === 'create' ? 'Create account' : 'Save account changes' }))
    await waitFor(() => expect(write).toHaveBeenCalledTimes(1))
    fireEvent.click(screen.getAllByRole('link', { name: 'Cancel' })[0])
    const destination = mode === 'create' ? '/admin/users' : '/admin/users/100'
    expect(screen.getByText(`Current path: ${destination}`)).toBeInTheDocument()
    const entry = screen.getByText(/^Current entry:/).textContent
    await act(async () => { save.resolve(response(detail)) })
    expect(screen.getByText(`Current path: ${destination}`)).toBeInTheDocument()
    expect(screen.getByText(/^Current entry:/)).toHaveTextContent(entry!)
  })
})
